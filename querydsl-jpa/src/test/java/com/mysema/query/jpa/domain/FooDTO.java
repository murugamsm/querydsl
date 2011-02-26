/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.jpa.domain;

import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.mysema.query.annotations.QueryProjection;

public class FooDTO {
    String bar;

    @Id
    int id;

    @ElementCollection
    List<String> names;

    @Temporal(TemporalType.DATE)
    java.util.Date startDate;

    public FooDTO() {
    }

    @QueryProjection
    public FooDTO(long l) {
    }

    @QueryProjection
    public FooDTO(long l, long r) {
    }
}
