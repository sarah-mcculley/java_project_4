package com.sarah;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

class HttpException extends IOException{
    public HttpException(HttpResponse response){
        super(response.getStatusLine().getStatusCode()
                + ": " + response.getStatusLine().getReasonPhrase());
    }
}

class HttpRequests{

    private CloseableHttpClient client;

    public HttpRequests(String username, String password){
        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        client = HttpClientBuilder.create().setDefaultCredentialsProvider(credentialsProvider).build();
    }

    private static boolean isSuccessful(HttpResponse response){
        int statusCode = response.getStatusLine().getStatusCode();
        return (statusCode >= 200 && statusCode <300);
    }

    private String makeRequest(HttpRequestBase request) throws IOException{
        CloseableHttpResponse response = client.execute(request);
        try {
            if (!isSuccessful(response)){
                throw new HttpException(response);
            }
            return EntityUtils.toString(response.getEntity());
        }
        finally {
            response.close();
        }
    }

    private void addData(HttpEntityEnclosingRequestBase request,
                         String contentType, String data) throws IOException{
        StringEntity requestData = new StringEntity(data);
        request.setEntity(requestData);
        request.setHeader("Content-type", contentType);
    }


    public String get(String url)throws IOException{
        HttpGet request = new HttpGet(url);
        return makeRequest(request);
    }

    public String delete(String url) throws IOException{
        HttpDelete request = new HttpDelete(url);
        return makeRequest(request);
        }



    public String put (String url, String contentType, String data) throws IOException{
        HttpPut request = new HttpPut(url);
        addData(request, contentType, data);
        return makeRequest(request);
    }

    public String post(String url, String contentType, String data) throws IOException{
        HttpPost request = new HttpPost(url);
        addData(request, contentType, data);
        return makeRequest(request);
    }

}

class Todo {
    private String title;
    private String body;
    private int priority;
    private Integer id = null;

    public Todo(String title, String body, int priority){
        this.title = title;
        this.body = body;
        this.priority = priority;
    }

    public Integer getId(){
        return id;
    }

    public String getTitle(){
        return title;
    }

    @Override
    public String toString(){
        return "TODO: ID: " + id + ", Title: " + title
                + ", Body: " + body + ", Priority: " + priority;
    }
}

class TodoCollection implements Iterable<Todo> {
    private List<Todo> todos;

    @Override
    public Iterator<Todo> iterator(){
        return todos.iterator();
    }
}

class TodoAPIWrapper{
    private Gson gson = new Gson();
    private HttpRequests requests;
    private String hostURL;

    public TodoAPIWrapper(String username, String password, String hostURL){
        requests = new HttpRequests(username, password);
        this.hostURL = hostURL;
    }

    public TodoCollection getTodos( ){
        String url = hostURL +"/todos/api/v1.0/todos";
        try {
            String response = requests.get(url);
            return gson.fromJson(response, TodoCollection.class);
        }
        catch (IOException e) {
            System.out.println("Unable to get todos.");
        }
        return null;

    }

    public Todo createTodo(Todo newTodo) {
        String url = hostURL + "/todos/api/v1.0/todo/create";
        String data = gson.toJson(newTodo);
        try {
            String response = requests.post(url, "application/json", data);
            return gson.fromJson(response, Todo.class);
        }
        catch (IOException e) {
            System.out.println("Unable to upload todo with title: " + newTodo.getTitle());
        }
        return null;
    }

    public boolean deleteTodo(int id){
        String url = hostURL + "/todos/api/v1.0/todo/delete/" + id;
        try {
            requests.delete(url);
            return true;
        }
        catch (IOException e) {
            System.out.println("Unable to delete todo with ID " + id);
        }
        return false;
    }


    public Todo createTodo(String title, String body, int priority) {
        Todo newTodo = new Todo(title, body, priority);
        return createTodo(newTodo);
    }
}

public class Main {
    public static void main(String[] args) {
        TodoAPIWrapper wrapper = new TodoAPIWrapper("test", "test",
                "http://todo.eastus.cloudapp.azure.com/todo-android");
        Todo newTodo = wrapper.createTodo("Class", "Finish class", 6);
        System.out.println("New ID: " + newTodo.getId());


        System.out.println("Removing todo");
        wrapper.deleteTodo(2);

        System.out.println("ALL TODOS");
        TodoCollection todos = wrapper.getTodos();
        for (Todo todo: todos) {
            System.out.println(todo);
        }
    }
}
