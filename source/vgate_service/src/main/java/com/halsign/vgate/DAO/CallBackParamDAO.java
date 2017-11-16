package com.halsign.vgate.DAO;

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CallBackParamDAO extends GenericDAO<Reader> {

	protected CallBackParamDAO(Connection con) {
		super(con);
	}
	
	public void insert(String id, Reader reader) throws SQLException {
		String req = "INSERT INTO vgatedb.callbackparam (ID, VALUE) VALUES (?, ?)";
		PreparedStatement pStmt = con.prepareStatement(req);
		pStmt.setString(1, id);
		pStmt.setCharacterStream(2, reader);
		pStmt.executeUpdate();
	}
	
	public Reader get(String id) throws SQLException {
		Reader ret = null;
		String req = "SELECT ID, VALUE FROM vgatedb.callbackparam WHERE ID = ?";
		PreparedStatement pStmt = con.prepareStatement(req);
		pStmt.setString(1, id);
		ResultSet rs = pStmt.executeQuery();
		while (rs.next()) {
			ret = rs.getCharacterStream("VALUE");
		}
		return ret;
	}

}
