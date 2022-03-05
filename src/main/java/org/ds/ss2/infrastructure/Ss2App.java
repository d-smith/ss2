package org.ds.ss2.infrastructure;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Ss2App {
    public static void main(final String[] args) {
        App app = new App();

        Environment env = Environment.builder()
                .account(System.getenv("PA_ACCOUNT_NO"))
                .region(System.getenv("AWS_REGION"))
                .build();

        new Ss2Stack(app, "Ss2Stack", StackProps.builder()
                .env(env)
                .build());

        app.synth();
    }
}

