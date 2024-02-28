package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class CrptApi {
    public static class RequestBody {
        private String document;
        private String signature;

        public RequestBody(String document, String signature) {
            this.document = document;
            this.signature = signature;
        }

        public String getDocument() {
            return document;
        }

        public void setDocument(String document) {
            this.document = document;
        }

        public String getSignature() {
            return signature;
        }

        public void setSignature(String signature) {
            this.signature = signature;
        }
    }

    public static class ResponseBody {
        private String status;
        private String message;

        public ResponseBody(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private final int requestLimit;
    private final long timeIntervalInMillis;
    private int currentRequestCount;
    private long lastResetTimeInMillis;
    private ReentrantLock lock;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.requestLimit = requestLimit;
        this.timeIntervalInMillis = timeUnit.toMillis(1);
        this.currentRequestCount = 0;
        this.lastResetTimeInMillis = System.currentTimeMillis();
        this.lock = new ReentrantLock();
    }

    public void createDocument(String document, String signature) {
        lock.lock();
        try {
            long currentTimeInMillis = System.currentTimeMillis();
            if (currentTimeInMillis - lastResetTimeInMillis >= timeIntervalInMillis) {
                // Если прошло достаточно времени, сбрасываем счетчик запросов и время последнего сброса
                lastResetTimeInMillis = currentTimeInMillis;
                currentRequestCount = 0;
            }

            if (currentRequestCount < requestLimit) {
                // Если количество текущих запросов меньше лимита, выполняем запрос к API
                currentRequestCount++;
                // Реализация отправки запроса к API Честного знака
                sendApiRequest(document, signature);
            } else {
                // Если превышен лимит запросов, ждем до следующего временного интервала
                try {
                    long timeToWait = lastResetTimeInMillis + timeIntervalInMillis - currentTimeInMillis;
                    TimeUnit.MILLISECONDS.sleep(timeToWait);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // После ожидания рекурсивно вызываем метод createDocument с теми же входными параметрами
                createDocument(document, signature);
            }
        } finally {
            lock.unlock();
        }
    }

    private void sendApiRequest(String document, String signature) {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://ismp.crpt.ru/api/v3/lk/documents/create");

        try {
            // Установка заголовков запроса
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer your_token"); // Заменяем значение на наш токен авторизации

            // Создание JSON-объекта из документа и подписи
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(
                    new RequestBody(document, signature)
            );

            // Установка тела запроса
            StringEntity requestEntity = new StringEntity(jsonBody);
            httpPost.setEntity(requestEntity);

            // Выполнение запроса
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                // Обработка успешного ответа
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                System.out.println("API Response: " + responseBody);
            } else {
                // Обработка ошибочного ответа
                System.out.println("API Request failed with status code: " + statusCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5);
        RequestBody requestBody = new RequestBody("documentJSON", "documentSignature");
        ResponseBody responseBody = new ResponseBody("success", "Request processed successfully");

        // Присваивание значений и чтение значений полей
        requestBody.setDocument("newDocumentJSON");
        String document = requestBody.getDocument();

        // Получение и установка значений полей ResponseBody
        String status = responseBody.getStatus();
        responseBody.setMessage("New message");
    }

}