package com.halsign.vgate.DAO;

import java.sql.Connection;

public abstract class GenericDAO<T> {

    //Protected
    protected Connection con;

    protected GenericDAO(Connection con) {
        this.con = con;
    }
}