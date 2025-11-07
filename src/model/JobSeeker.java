package model;

public class JobSeeker extends User {
    public JobSeeker(String username, String password) {
        super(username, password, "Candidate");
    }
}