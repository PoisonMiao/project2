package com.ifchange.tob.common.rabbit;

public interface IRabbitRouter {
    String DIRECT = "direct", FANOUT = "fanout";

    String exchange();

    String exType();

    String queue();

    boolean durable();
}
