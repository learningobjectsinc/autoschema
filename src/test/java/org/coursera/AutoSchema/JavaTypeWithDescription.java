package org.coursera.AutoSchema;

import org.coursera.autoschema.annotations.Description;

@Description("Type description")
public class JavaTypeWithDescription {
    private String param;

    @Description("Parameter description")
    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}
