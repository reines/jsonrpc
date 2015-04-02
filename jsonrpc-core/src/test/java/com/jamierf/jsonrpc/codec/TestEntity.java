package com.jamierf.jsonrpc.codec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class TestEntity {

    private final String stringValue;
    private final int intValue;

    @JsonCreator
    public TestEntity(
            @JsonProperty("string") final String stringValue,
            @JsonProperty("int") final int intValue) {
        this.stringValue = stringValue;
        this.intValue = intValue;
    }

    public String getString() {
        return stringValue;
    }

    public int getInt() {
        return intValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestEntity that = (TestEntity) o;
        return Objects.equal(intValue, that.intValue) &&
                Objects.equal(stringValue, that.stringValue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stringValue, intValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("stringValue", stringValue)
                .add("intValue", intValue)
                .toString();
    }
}
