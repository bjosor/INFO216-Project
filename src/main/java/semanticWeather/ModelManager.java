package semanticWeather;

import com.github.jsonldjava.utils.Obj;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;

public class ModelManager {

    //custom @context for interpreting JSON-LD from the frost api.
    static String jsonContext = "{\"schema\": \"http://schema.org/\"," +
            "\"xsd\": \"http://www.w3.org/2001/XMLSchema#\"," +
            "\"frost\": \"https://frost.met.no/schema#\"," +
            "\"SensorSystem\": {\"@id\":\"frost:SensorSystem\"," +
                "\"@type\":\"frost:SensorSystem\"}," +
            "\"observations\": {\"@id\":\"frost:Observation\"," +
                "\"@type\":\"frost:Observation\"}," +
            "\"level\": {\"@id\":\"frost:level\"," +
                "\"@type\":\"frost:level\"}," +
            "\"referenceTime\": {\"@id\":\"xsd:dateTime\"," +
                "\"@type\":\"xsd:dateTime\"}," +
            "\"value\": {\"@id\":\"frost:value\"," +
                "\"@type\":\"xsd:decimal\"}," +
            "\"id\": {\"@id\":\"frost:sensorId\"}," +
            "\"sourceId\": {\"@id\":\"frost:sensorId\"}," +
            "\"elementId\": {\"@id\":\"frost:elementId\"}," +
            "\"name\": {\"@id\":\"frost:name\"}," +
            "\"municipality\": {\"@id\":\"frost:municipality\"}," +
            "\"county\": {\"@id\":\"frost:county\"}" +
            "}";

   static String auth = "477f0363-b58e-4508-8231-43b84dd51600";

    private static ModelManager instance;
    private static Model model;

    /**
     * read the model from file. If none exists
     * create a new one with a request for SensorStations
     */
    private ModelManager(){
        model = ModelFactory.createDefaultModel();

        try{
            model.read("database.json", "JSON-LD");
        }catch (Exception e){
            System.out.println("No database file found");
            apiGetSources();
        }
    }

    /**
     * singleton constructor
     * @return
     */
    public static ModelManager getInstance(){
        if (instance == null){
            instance = new ModelManager();
        }
        return instance;
    }

    /**
     * Requests data from the specified url with the given authentication.
     * @param auth
     * @param request url for the request
     * @return
     */
    public HttpResponse<JsonNode> getWeatherJson(String auth, String request){
        // I use unirest to make the request to the api
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.get(request).
                    basicAuth(auth, "").
                    asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        System.out.println(response.getBody().toString());
        return response;
    }

    /**
     * Parses the jsonString into a suitable format
     * for reading into the model
     * @param jsonString
     * @param model
     */
    public void addJsonToModel(String jsonString, Model model) {
        JSONParser parser = new JSONParser();
        try {

            JSONObject jsonObj = (JSONObject) parser.parse(jsonString);
            JSONArray data = (JSONArray) jsonObj.get("data");

            for (int i = 0; i < data.size(); i++) {
                JSONObject temp = (JSONObject)data.get(i);

                if( temp.get("sourceId") != null){
                    String id = temp.get("sourceId").toString();
                    temp.replace("sourceId", id.substring(0, id.length()-2));
                }

                String jsonStr = temp.toString();
                jsonStr = jsonStr.substring(0, jsonStr.length() - 1) + ",\"@context\":" + jsonContext + "}";

                InputStream targetStream = new ByteArrayInputStream(jsonStr.getBytes());
                model.read(targetStream, "", "JSON-LD");
            }

            FileWriter out = new FileWriter("database.json");
            FileWriter outXml = new FileWriter("database.xml");
            try{
                System.out.println("Writing model to file..");
                model.write(out, "JSON-LD");
                model.write(outXml, "RDF/XML");
            } finally {
                try {
                    out.close();
                } catch (IOException closeException){
                    System.out.println("closeException");
                }
            }
            System.out.println("Model written.");

        } catch (Exception e) {
            System.out.println("Error reading into model.");
            System.out.println(e.toString());
            //e.printStackTrace();
        }
    }

    /**
     * sparql query to the model for sensorstations.
     * @param sensorID Id or part of an id to match for
     * @param name matches for values in the name county and municipality properties
     * @return the query result as an ArrayList of Records
     */
    public ArrayList<Record> query(String sensorID, String name) {

        String strQuery = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
                "PREFIX frost: <https://frost.met.no/schema#>" +
                "PREFIX schema: <https://schema.org/>" +
                "" +
                "SELECT ?sensor ?id ?name ?muni ?county \n" +
                "WHERE \n" +
                "{ \n" +
                "?sensor frost:sensorId ?id . FILTER regex(?id, \"" + sensorID + "\", 'i') ." +
                "?sensor frost:name ?name . " +
                "?sensor frost:municipality ?muni ." +
                "?sensor frost:county ?county ." +
                "FILTER (regex(?muni, \"" + name + "\", 'i') || regex(?name, \"" + name + "\", 'i') || regex(?county, \"" + name + "\", 'i'))" +
                "}" +
                "ORDER BY (?id)";

        ResultSet results = execute(strQuery);

        ArrayList<Record> records  = new ArrayList<>();

        while(results.hasNext()){
            QuerySolution t = results.nextSolution();

            String idResult = t.get("id").toString();
            String nameResult = t.get("name").toString();
            String muniResult = t.get("muni").toString();
            String countyResult = t.get("county").toString();

            records.add(new Record(idResult,nameResult,countyResult ,muniResult,null, null, null));
        }
        return records;

    }

    /**
     * sparql query to the model for Observations
     * @param sensorID Id or part of an id to match for
     * @param name matches for values in the name county and municipality properties
     * @param element matches for the given element
     * @param fromDate matches instances between this date and the fromDate
     * @param toDate matches instances between this date and the toDate
     * @return the query results as an ArrayList of Records
     */
    public ArrayList<Record> query(String sensorID, String name, Object element, LocalDate fromDate, LocalDate toDate) {
        String fromDateStr = "";
        String toDateStr = "";
        String elementStr = "";

        if(fromDate != null && toDate != null){
            fromDateStr = fromDate.toString() + "T00:00:00Z";
            toDateStr = toDate.toString() + "T00:00:00Z";
        }

        if (element != null){
            elementStr = element.toString();
        }

        elementStr = elementStr.replace("(", "\\\\(");
        elementStr = elementStr.replace(")", "\\\\)");

        String strQuery = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
                "PREFIX frost: <https://frost.met.no/schema#>" +
                "PREFIX schema: <https://schema.org/>" +
                "" +
                "SELECT ?sensor ?id ?name ?muni ?county ?date ?element ?val\n" +
                "WHERE \n" +
        "{ \n" +
                "?sensor frost:sensorId ?id . FILTER regex(?id, \"" + sensorID + "\", 'i') ." +
                "?sensor frost:name ?name . " +
                "?sensor frost:municipality ?muni ." +
                "?sensor frost:county ?county ." +
                "FILTER (regex(?muni, \"" + name + "\", 'i') || regex(?name, \"" + name + "\", 'i') || regex(?county, \"" + name + "\", 'i'))" +
                "?obsGroup frost:sensorId ?id ." +
                "?obsGroup xsd:dateTime ?date . ";

        if(fromDate != null && toDate != null){
            strQuery += "FILTER (?date >= \"" + fromDateStr + "\"^^xsd:dateTime && ?date <= \"" + toDateStr + "\"^^xsd:dateTime)  . \n";
        }

        strQuery += "?obsGroup frost:Observation ?o . " +
                "?o frost:elementId ?element . FILTER (regex(?element, \"" + elementStr + "\", 'i'))" +
                "?o frost:value ?val . " +
                "}" +
                "ORDER BY(?date)" +
                "LIMIT 5000";

        ResultSet results = execute(strQuery);

        ArrayList<Record> records  = new ArrayList<>();

        while(results.hasNext()){
            QuerySolution t = results.nextSolution();

            String idResult = t.get("id").toString();
            String nameResult = t.get("name").toString();
            String muniResult = t.get("muni").toString();
            String countyResult = t.get("county").toString();
            String elementResult = t.get("element").toString();
            String valResult = t.get("val").toString();
            String dateResult = formatTemp(t.get("date").toString());
            Float temperature = Float.parseFloat(formatTemp(valResult));

            records.add(new Record(idResult, nameResult, countyResult, muniResult,elementResult, dateResult, temperature.toString()));
        }
        return records;
    }

    /**
     * formats a temperature value into a more readable format
     * @param temp
     * @return
     */
    public String formatTemp(String temp){
        String result = temp.split("\\^")[0];
        return result;
    }

    /**
     * executes the given sparql query
     * @param strQuery query to execute
     * @return results as a ResultSet
     */
    private ResultSet execute(String strQuery){
        Query newQuery = QueryFactory.create(strQuery);
        String finalQuery = newQuery.serialize();

        System.out.println(finalQuery);

        QueryExecution qexec = QueryExecutionFactory.create(finalQuery, model);
        ResultSet results = qexec.execSelect();

        return results;
    }

    /**
     * takes a given array of parameters and formats these into an api request
     * @param params array of string parameters
     * @return formatted url
     */
    private String formatApiRequest(String[] params){
        String request = "https://frost.met.no/observations/v0.jsonld?";

        for (String s: params) {
            if (s.length() > 1){
                request += s + "&";
            }
        }
        request = request.substring(0,request.length()-1);
        return request;
    }

    /**
     * executes steps necessary to get observations from the api into the model.
     * @param strings parameters to request information for
     */
    public void apiGetObservations(String[] strings) {
        String url = formatApiRequest(strings);

        String obsResponse = getWeatherJson(auth, url).getBody().toString();
        obsResponse = obsResponse.replace("\"@context\":\"https://frost.met.no/schema\",", "\"@context\":" + jsonContext + ",");

        addJsonToModel(obsResponse, model);
    }

    /**
     * executes steps necessary to get the list of sensorstations in norway into the model
     */
    public void apiGetSources() {
        String url = "https://frost.met.no/sources/v0.jsonld?country=Norge";

        String obsResponse = getWeatherJson(auth, url).getBody().toString();
        obsResponse = obsResponse.replace("\"@context\":\"https://frost.met.no/schema\",", "\"@context\":" + jsonContext + ",");

        addJsonToModel(obsResponse, model);
    }
}
















