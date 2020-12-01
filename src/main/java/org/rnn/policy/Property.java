package org.rnn.policy;

import org.rnn.json.JSONObject;

/**
 * @author Piotr Frohlich
 * @version 1.0.1
 * This class is a representation of the single Property assigned to a subject
 * in the network.
 */
public class Property
{
    /**
     * The subject to which the property is assigned to.
     */
    private String subject;

    /**
     * Name of the property. E.g. GEOLOCATION. Please note that this name isn't
     * case-sensitive.
     */
    private String name;

    /**
     * The value of the property. E.g. CHINA
     */
    private String value;

    /**
     * Basic constructor. Initialized every field.
     * @param s Subject
     * @param n Name
     * @param v Value
     */
    public Property(String s, String n, String v) {
        subject = s;
        name = n;
        value = v;
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public JSONObject jsonify()
    {
        JSONObject propertyJ = new JSONObject();
        propertyJ.put("subject",subject).put("name",name).put("value",value);
        return propertyJ;
    }

}
