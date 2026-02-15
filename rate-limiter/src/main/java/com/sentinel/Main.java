package com.sentinel;

public class Main {
    public static void main( String[] args ) {
        RateLimiterService service = new RateLimiterService(10, 1);
        for (int i = 0; i<= 15; i++) {
            boolean allowed = service.allowRequest("User1");
            System.out.println("Request " + i + ": " + (allowed ? "Allowed" : "Blocked"));
        }
    }
}
