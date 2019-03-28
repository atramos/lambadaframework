package org.lambadaframework.runtime;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.MethodHandler;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lambadaframework.runtime.models.Request;
import org.lambadaframework.runtime.models.RequestInterface;
import org.lambadaframework.runtime.models.ResponseProxy;
import org.lambadaframework.runtime.router.Router;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Invocable.class, ResourceMethod.class, Router.class, org.lambadaframework.jaxrs.model.ResourceMethod.class})
public class HandlerTest {


    public static class Entity {
        public long id;
        public String query1;
        public String requestBody;
    }


    public static class NewEntityRequest {
        public long id;
    }

    @Path("/")
    public static class DummyController {
        @GET
        @Path("{id}")
        public javax.ws.rs.core.Response getEntity(
                @PathParam("id") long id
        ) {
            Entity entity = new Entity();
            entity.id = id;
            entity.query1 = "cagatay gurturk";
            return javax.ws.rs.core.Response
                    .status(200)
                    .entity(entity)
                    .build();
        }

        @POST
        @Path("{id}")
        public javax.ws.rs.core.Response createEntity(
                @PathParam("id") long id,
                @QueryParam("query1") String query1
        ) {
            Entity entity = new Entity();
            entity.id = id;
            entity.query1 = query1;

            return javax.ws.rs.core.Response
                    .status(201)
                    .header("Location", "http://www.google.com")
                    .entity(entity)
                    .build();
        }

        @POST
        @Path("{id}/jsonstring")
        @Consumes(MediaType.APPLICATION_JSON)
        public javax.ws.rs.core.Response createEntityWithJsonBody(
                String jsonString
        ) {

            return javax.ws.rs.core.Response
                    .status(201)
                    .entity(jsonString)
                    .build();
        }

        @POST
        @Path("{id}/jsonobject")
        @Consumes(MediaType.APPLICATION_JSON)
        public javax.ws.rs.core.Response createEntityWithJsonObject(
                NewEntityRequest jsonEntity
        ) {

            return javax.ws.rs.core.Response
                    .status(201)
                    .entity(jsonEntity)
                    .build();
        }

        @POST
        @Path("{id}/error")
        @Consumes(MediaType.APPLICATION_JSON)
        public javax.ws.rs.core.Response createEntityWithStatus401(
                String jsonString
        ) {

            return javax.ws.rs.core.Response
                    .status(401)
                    .entity(jsonString)
                    .build();
        }
    }

    private Router getMockRouter(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {

        Invocable mockInvocable = PowerMock.createMock(Invocable.class);
        expect(mockInvocable.getHandlingMethod())
                .andReturn(DummyController.class.getDeclaredMethod(methodName, parameterTypes))
                .anyTimes();

        expect(mockInvocable.getHandler())
                .andReturn(MethodHandler.create(DummyController.class))
                .anyTimes();

        org.lambadaframework.jaxrs.model.ResourceMethod mockResourceMethod = PowerMock.createMock(org.lambadaframework.jaxrs.model.ResourceMethod
                .class);
        expect(mockResourceMethod.getInvocable())
                .andReturn(mockInvocable)
                .anyTimes();

        Router mockRouter = PowerMock.createMock(Router.class);
        expect(mockRouter.route(anyObject()))
                .andReturn(mockResourceMethod)
                .anyTimes();

        PowerMock.replayAll();
        return mockRouter;
    }

    private Request getRequest(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Request.class);
    }

    private Context getContext() {
        return new Context() {
            @Override
            public String getAwsRequestId() {
                return "23234234";
            }

            @Override
            public String getLogGroupName() {
                return null;
            }

            @Override
            public String getLogStreamName() {
                return null;
            }

            @Override
            public String getFunctionName() {
                return null;
            }

            @Override
            public String getFunctionVersion() {
                return null;
            }

            @Override
            public String getInvokedFunctionArn() {
                return null;
            }

            @Override
            public CognitoIdentity getIdentity() {
                return null;
            }

            @Override
            public ClientContext getClientContext() {
                return null;
            }

            @Override
            public int getRemainingTimeInMillis() {
                return 5000;
            }

            @Override
            public int getMemoryLimitInMB() {
                return 128;
            }

            @Override
            public LambdaLogger getLogger() {
                return null;
            }
        };
    }

    @Test
    public void dummyTest() {
        assertTrue(true);
    }

    @Test
    public void testParseRequestWithJacksson() throws Exception {

        InputStream jsonAsInputStream = getJsonAsInputStream();
        loggInput(jsonAsInputStream);

        Handler handler = new Handler();
        RequestInterface req = handler.getParsedRequest(jsonAsInputStream);

        assertEquals("GET", req.getMethod().name());
        assertEquals("/test/hello", req.getPathTemplate());
        assertEquals("me", req.getQueryParams().get("name"));
    }

    private JSONObject parseResponse(String json) {

        JSONObject responseJson = new JSONObject();

        JSONObject responseBody = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(json);
        responseBody.put("input", jsonArray);
        responseJson.put("body", responseBody);

        return responseBody;
    }

    private void loggInput(InputStream inputStream) {

        JSONParser parser = new JSONParser();
        try {
            final JSONObject parse = (JSONObject) parser.parse(new BufferedReader(new InputStreamReader(inputStream)));
            inputStream.reset();
            System.out.println("parse = " + parse);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }

    }

    private InputStream getJsonAsInputStream() {
        String json = "{\n" +
                "        \"path\": \"/test/hello\",\n" +
                "        \"headers\": {\n" +
                "            \"Accept\": \"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8\",\n" +
                "            \"Accept-Encoding\": \"gzip, deflate, lzma, sdch, br\",\n" +
                "            \"Accept-Language\": \"en-US,en;q=0.8\",\n" +
                "            \"CloudFront-Forwarded-Proto\": \"https\",\n" +
                "            \"CloudFront-Is-Desktop-Viewer\": \"true\",\n" +
                "            \"CloudFront-Is-Mobile-Viewer\": \"false\",\n" +
                "            \"CloudFront-Is-SmartTV-Viewer\": \"false\",\n" +
                "            \"CloudFront-Is-Tablet-Viewer\": \"false\",\n" +
                "            \"CloudFront-Viewer-Country\": \"US\",\n" +
                "            \"Host\": \"wt6mne2s9k.execute-api.us-west-2.amazonaws.com\",\n" +
                "            \"Upgrade-Insecure-Requests\": \"1\",\n" +
                "            \"User-Agent\": \"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36 OPR/39.0.2256.48\",\n" +
                "            \"Via\": \"1.1 fb7cca60f0ecd82ce07790c9c5eef16c.cloudfront.net (CloudFront)\",\n" +
                "            \"X-Amz-Cf-Id\": \"nBsWBOrSHMgnaROZJK1wGCZ9PcRcSpq_oSXZNQwQ10OTZL4cimZo3g==\",\n" +
                "            \"X-Forwarded-For\": \"192.168.100.1, 192.168.1.1\",\n" +
                "            \"X-Forwarded-Port\": \"443\",\n" +
                "            \"X-Forwarded-Proto\": \"https\"\n" +
                "        },\n" +
                "        \"pathParameters\": {\"id\": \"1234\"},\n" +
                "        \"httpMethod\": \"GET\",\n" +
                "        \"queryStringParameters\": {\"name\": \"me\"} \n" +
                "    }";

        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testWith200Result()
            throws Exception {


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("getEntity", long.class));
        ByteArrayOutputStream boas = new ByteArrayOutputStream();


        handler.handleRequest(getJsonAsInputStream(), boas, getContext());

        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(boas.toString());
        JSONObject body = (JSONObject) parser.parse((String) json.get("body"));
        assertEquals(1234L, body.get("id"));
        assertEquals("cagatay gurturk", body.get("query1"));

    }

/*
    @Test
    public void testWith201Result()
            throws Exception {


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntity", long.class, String.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals("test3", ((Entity) response.getEntity()).query1);
        assertEquals(123, ((Entity) response.getEntity()).id);
        assertEquals("http://www.google.com", response.getHeaders().get("Location"));

    }


    @Test
    public void testWithJsonBodyAsString201Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"test\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntityWithJsonBody", String.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals("test", response.getEntity());
    }


    @Test
    public void testWithJsonAsObject201Result()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"{\\\"id\\\":1}\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntityWithJsonObject", NewEntityRequest.class));
        Response response = handler.handleRequest(exampleRequest, getContext());

        assertEquals("201", response.getErrorMessage());
        assertEquals(1, ((NewEntityRequest) response.getEntity()).id);
    }

    @Test
    public void testWith401ExceptionResult()
            throws Exception {

        Request exampleRequest = getRequest("{\n" +
                "  \"package\": \"org.lambadaframework\",\n" +
                "  \"pathTemplate\": \"/{id}/error\",\n" +
                "  \"method\": \"POST\",\n" +
                "  \"requestBody\": \"{}\",\n" +
                "  \"path\": {\n" +
                "    \"id\": \"123\"\n" +
                "  },\n" +
                "  \"querystring\": {\n" +
                "        \"query1\": \"test3\",\n" +
                "    \"query2\": \"test\"\n" +
                "  },\n" +
                "  \"header\": {}\n" +
                "}");


        Handler handler = new Handler();
        handler.setRouter(getMockRouter("createEntityWithStatus401", String.class));
        try {
            handler.handleRequest(exampleRequest, getContext());
            fail("Should have thrown an excpetion");
        } catch(RuntimeException e) {
            assertTrue(e.getMessage().contains("401"));
        }
    }*/
    
    @Test public void testWithProxyInput() throws UnsupportedEncodingException {
    	String exampleRequest = "{\r\n" + 
    			"    \"resource\": \"/{name}\",\r\n" + 
    			"    \"path\": \"/xxx\",\r\n" + 
    			"    \"httpMethod\": \"GET\",\r\n" + 
    			"    \"headers\": {\r\n" + 
    			"        \"Accept\": \"*/*\",\r\n" + 
    			"        \"CloudFront-Forwarded-Proto\": \"https\",\r\n" + 
    			"        \"CloudFront-Is-Desktop-Viewer\": \"true\",\r\n" + 
    			"        \"CloudFront-Is-Mobile-Viewer\": \"false\",\r\n" + 
    			"        \"CloudFront-Is-SmartTV-Viewer\": \"false\",\r\n" + 
    			"        \"CloudFront-Is-Tablet-Viewer\": \"false\",\r\n" + 
    			"        \"CloudFront-Viewer-Country\": \"US\",\r\n" + 
    			"        \"Host\": \"ftchy55dmc.execute-api.us-west-1.amazonaws.com\",\r\n" + 
    			"        \"User-Agent\": \"curl/7.55.1\",\r\n" + 
    			"        \"Via\": \"1.1 05aec04162b0fed6e9762cd1edd66a72.cloudfront.net (CloudFront)\",\r\n" + 
    			"        \"X-Amz-Cf-Id\": \"rKHblgFtqBvzzM8WUDjDZknLR7oxzd0n2q7V-vtsrYaMTIj8yCZHUQ==\",\r\n" + 
    			"        \"X-Amzn-Trace-Id\": \"Root=1-5c99c0f6-f9a32675a1c140f5810d9950\",\r\n" + 
    			"        \"X-Forwarded-For\": \"99.99.99.99, 70.132.18.81\",\r\n" + 
    			"        \"X-Forwarded-Port\": \"443\",\r\n" + 
    			"        \"X-Forwarded-Proto\": \"https\"\r\n" + 
    			"    },\r\n" + 
    			"    \"multiValueHeaders\": {\r\n" + 
    			"        \"Accept\": [\r\n" + 
    			"            \"*/*\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"CloudFront-Forwarded-Proto\": [\r\n" + 
    			"            \"https\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"CloudFront-Is-Desktop-Viewer\": [\r\n" + 
    			"            \"true\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"CloudFront-Is-Mobile-Viewer\": [\r\n" + 
    			"            \"false\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"CloudFront-Is-SmartTV-Viewer\": [\r\n" + 
    			"            \"false\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"CloudFront-Is-Tablet-Viewer\": [\r\n" + 
    			"            \"false\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"CloudFront-Viewer-Country\": [\r\n" + 
    			"            \"US\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"Host\": [\r\n" + 
    			"            \"ftchy55dmc.execute-api.us-west-1.amazonaws.com\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"User-Agent\": [\r\n" + 
    			"            \"curl/7.55.1\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"Via\": [\r\n" + 
    			"            \"1.1 05aec04162b0fed6e9762cd1edd66a72.cloudfront.net (CloudFront)\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"X-Amz-Cf-Id\": [\r\n" + 
    			"            \"rKHblgFtqBvzzM8WUDjDZknLR7oxzd0n2q7V-vtsrYaMTIj8yCZHUQ==\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"X-Amzn-Trace-Id\": [\r\n" + 
    			"            \"Root=1-5c99c0f6-f9a32675a1c140f5810d9950\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"X-Forwarded-For\": [\r\n" + 
    			"            \"99.99.99.99, 70.132.18.81\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"X-Forwarded-Port\": [\r\n" + 
    			"            \"443\"\r\n" + 
    			"        ],\r\n" + 
    			"        \"X-Forwarded-Proto\": [\r\n" + 
    			"            \"https\"\r\n" + 
    			"        ]\r\n" + 
    			"    },\r\n" + 
    			"    \"queryStringParameters\": null,\r\n" + 
    			"    \"multiValueQueryStringParameters\": null,\r\n" + 
    			"    \"pathParameters\": {\r\n" + 
    			"        \"name\": \"xxx\"\r\n" + 
    			"    },\r\n" + 
    			"    \"stageVariables\": null,\r\n" + 
    			"    \"requestContext\": {\r\n" + 
    			"        \"resourceId\": \"j6ha2i\",\r\n" + 
    			"        \"resourcePath\": \"/{name}\",\r\n" + 
    			"        \"httpMethod\": \"GET\",\r\n" + 
    			"        \"extendedRequestId\": \"XIsWfHknyK4FmNA=\",\r\n" + 
    			"        \"requestTime\": \"26/Mar/2019:06:04:38 +0000\",\r\n" + 
    			"        \"path\": \"/production/xxx\",\r\n" + 
    			"        \"accountId\": \"984073016564\",\r\n" + 
    			"        \"protocol\": \"HTTP/1.1\",\r\n" + 
    			"        \"stage\": \"production\",\r\n" + 
    			"        \"domainPrefix\": \"ftchy55dmc\",\r\n" + 
    			"        \"requestTimeEpoch\": 1553580278357,\r\n" + 
    			"        \"requestId\": \"09b858ef-4f8d-11e9-8d69-f54613b22d88\",\r\n" + 
    			"        \"identity\": {\r\n" + 
    			"            \"cognitoIdentityPoolId\": null,\r\n" + 
    			"            \"accountId\": null,\r\n" + 
    			"            \"cognitoIdentityId\": null,\r\n" + 
    			"            \"caller\": null,\r\n" + 
    			"            \"sourceIp\": \"99.99.99.99\",\r\n" + 
    			"            \"accessKey\": null,\r\n" + 
    			"            \"cognitoAuthenticationType\": null,\r\n" + 
    			"            \"cognitoAuthenticationProvider\": null,\r\n" + 
    			"            \"userArn\": null,\r\n" + 
    			"            \"userAgent\": \"curl/7.55.1\",\r\n" + 
    			"            \"user\": null\r\n" + 
    			"        },\r\n" + 
    			"        \"domainName\": \"ftchy55dmc.execute-api.us-west-1.amazonaws.com\",\r\n" + 
    			"        \"apiId\": \"ftchy55dmc\"\r\n" + 
    			"    },\r\n" + 
    			"    \"body\": null,\r\n" + 
    			"    \"isBase64Encoded\": false\r\n" + 
    			"}\r\n" + 
    			"";
    	
        Handler handler = new Handler();
        Router router = Router.getRouter();
        handler.setRouter(router);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final StringInputStream is = new StringInputStream(exampleRequest);
		final Context context = getContext();
		handler.handleRequest(is, os, context);
        
        final String out = new String(os.toByteArray());
		//Assert.assertTrue(out.startsWith("{\"path\":\"\\/test\\/hello\",\"headers\":{"));
        //FIXME
    }
    
    @Test
    public void testWithProxyOutput() throws JsonParseException, JsonMappingException, IOException {
        class Entity {
            public int id = 1;
            public String name;

            public Entity(String name) {
                this.name = name;
            }
        }
    	
    	Response response = Response.status(201)
                .entity(new Entity("xyz"))
                .header("Access-Control-Allow-Origin", "*")
                .header("X-Test", "YZ")
                .build();
    	
    	ResponseProxy responseProxy = ResponseProxy.buildFromJAXRSResponse(response);

    	Assert.assertEquals("{\"id\":1,\"name\":\"xyz\"}", new ObjectMapper().writeValueAsString(responseProxy.getEntity()));
    }

}