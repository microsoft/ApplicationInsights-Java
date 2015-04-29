package com.microsoft.applicationinsights.management.rest.model;

import net.minidev.json.JSONObject;

/**
 * Created by yonisha on 4/19/2015.
 * A class represents a subscription in Azure.
 */
public class Subscription {
    private String id;
    private String name;

    /**
     * Gets the subscription ID.
     * @return The subscription ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the subscription ID.
     * @param id The subscription ID.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the subscription name.
     * @return The subscription name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the subscription name.
     * @param name The subscription name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Converts the given subscription JSON object to Subscription object.
     * @param subscriptionJson The subscription JSON object.
     * @return Subscription object.
     */
    public static Subscription fromJSONObject(JSONObject subscriptionJson) {
        Subscription subscription = new Subscription();
        subscription.setId((String) subscriptionJson.get("subscriptionId"));
        subscription.setName((String) subscriptionJson.get("displayName"));

        return subscription;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        Subscription other = (Subscription) obj;

        return id != null && id.equals(other.getId());
    }
}
