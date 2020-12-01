package org.rnn.policy;

import com.google.common.base.Objects;
import org.rnn.json.JSONObject;

public class Criteria
{
    /**
     * Default criteria - set if field is uninitialized. Matches all properties.
     */
    public static String MATCH_ALL_CRITERIA = "NONE";

    /**
     * Subject from the network (such as forwarder, link, host, etc) on which a
     * given criteria must be fulfilled.
     */
    String subject;

    /**
     * Name of the criteria.
     */
    String name;

    /**
     * Type of the criteria (can be SOFT or HARD). SOFT criteria means that it's
     * preferred to match the criteria. HARD criteria means that it's mandatory
     * to match the criteria.
     */
    CRITERIA_TYPE TYPE;

    /**
     * {INT,DOUBLE}:value:{>,<,=} or plain text value. EG. INT:100:> stands for
     * criteria for @name must be greater than @value at @subject.
     */
    String value;

    /**
     * Enum describing the criteria type. It can be SOFT or HARD
     */
    public enum CRITERIA_TYPE
    {
        SOFT,HARD
    }

    /**
     * Response for the criteria can be OK, SOFT_NOT, HARD_NOT - for unfulfilled
     * SOFT criteria the response is SOFT_NOT. For HARD criteria the response is
     * HARD_NOT.
     */
    public enum CRITERIA_RESPONSE
    {
        OK,SOFT_NOT,HARD_NOT
    }

    /**
     * This method is called when the criteria are checked (the verdict wheather the
     * criteria is matched or not) is passed as a parameter.
     * @param verdict Wheater the criteria is matched or not.
     * @return A correct response
     */
    private CRITERIA_RESPONSE respond(boolean verdict)
    {
        if(verdict == true)
        {
            if(this.TYPE == CRITERIA_TYPE.SOFT)
            {
                return CRITERIA_RESPONSE.SOFT_NOT;
            }
            else
                return CRITERIA_RESPONSE.HARD_NOT;
        }
        else
            return CRITERIA_RESPONSE.OK;
    }

    /**
     * This method checks if the given property fulfills this criteria.
     * @param property Given property
     * @return A response
     */
    public CRITERIA_RESPONSE checkCriteria(Property property)
    {
        if(subject.equalsIgnoreCase(property.getSubject()))
        {
            if(name.equalsIgnoreCase(property.getName()))
            {
                if(value.contains(":"))
                {
                    try
                    {
                        String [] splitted = value.split(":");
                        if(splitted[0].equalsIgnoreCase("INT"))
                        {
                            int v = Integer.parseInt(splitted[1]);
                            if(splitted[2].equals("<"))
                            {
                                if(v < Integer.parseInt(property.getValue()))
                                {
                                    return respond(true);
                                }
                            }
                            else if(splitted[2].equals(">"))
                            {
                                if(v>Integer.parseInt(property.getValue()))
                                {
                                    return respond(true);
                                }
                            }
                            else if(splitted[2].equals("="))
                            {
                                if(v==Integer.parseInt(property.getValue()))
                                {
                                    return respond(true);
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        return CRITERIA_RESPONSE.OK;
                    }
                }
                else
                {
                    if(value.equalsIgnoreCase(MATCH_ALL_CRITERIA) || property.getValue().equalsIgnoreCase(MATCH_ALL_CRITERIA))
                        return CRITERIA_RESPONSE.OK;

                    if(value.contains("NOT"))
                    {
                        try {
                            String[] splitted = value.split("_");
                            if (splitted[1].equalsIgnoreCase(property.getValue()))
                            {
                                return respond(true);
                            }
                            return respond(false);
                        }
                        catch (Exception e)
                        {
                            return CRITERIA_RESPONSE.OK;
                        }
                    }
                    if(value.equalsIgnoreCase(property.getValue()))
                    {
                        return CRITERIA_RESPONSE.OK;
                    }
                    return respond(true);

                }
            }
        }
        return CRITERIA_RESPONSE.OK;
    }

    /**
     * Getter for the value of the criteria.
     * @return
     */
    public String getValue() {
        return value;
    }

    public String getSubject() {
        return subject;
    }

    public String getName() {
        return name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString()
    {
        return "TYPE : "+TYPE+" , SUBJECT : "+ subject + " , NAME: "+name+" , VALUE: "+value;

    }

    public void setTYPE(CRITERIA_TYPE TYPE) {
        this.TYPE = TYPE;
    }

    public JSONObject jsonify()
    {
        JSONObject criteria = new JSONObject();
        criteria.put("subject",subject)
                .put("name",name)
                .put("value",value)
                .put("type",TYPE);
        return criteria;
    }
}
