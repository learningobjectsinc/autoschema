package org.coursera.AutoSchema;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JavaType {
    private int _int;
    private Map<String, Integer> _str2Int;
    private List<Long> _jlist;
    private Optional<String> _jopt;

    public int getInt() {
        return _int;
    }

    public void setInt(int _intVal) {
        this._int = _intVal;
    }

    public List<Long> getJlist() {
        return _jlist;
    }

    public void setJlist(List<Long> jlist) {
        this._jlist = jlist;
    }

    public Optional<String> getJopt() {
        return _jopt;
    }

    public void setJopt(Optional<String> _jopt) {
        this._jopt = _jopt;
    }

    public Map<String, Integer> getStr2Int() {
        return _str2Int;
    }

    public void setStr2Int(Map<String, Integer> _str2IntVal) {
        this._str2Int = _str2IntVal;
    }
}

