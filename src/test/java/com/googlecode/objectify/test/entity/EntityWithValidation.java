package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.NotNullOrEmpty;
import com.googlecode.objectify.annotation.PossibleValues;
import com.googlecode.objectify.util.ValidatingEntity;

/**
 *
 * @author Hendrik Pilz <hepisec@gmail.com>
 */
@Entity
public class EntityWithValidation extends ValidatingEntity {
    @Id
    private Long id;
    @NotNullOrEmpty
    private String notNull;
    @PossibleValues({"one", "two", "three"})
    private String number;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNotNull() {
        return notNull;
    }

    public void setNotNull(String notNull) {
        this.notNull = notNull;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }    
}
