package com.halsign.vgate.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.halsign.vgate.Template;
import com.halsign.vgate.util.Encryptor;

public class TemplateDAO extends GenericDAO<Template> {

	protected TemplateDAO(Connection con) {
		super(con);
	}

	public List<Template> findTemplatesByGoldTemplate(String goldUUID) throws SQLException {
		List<Template> list = new ArrayList<Template>();
		PreparedStatement stm = this.con.prepareStatement("SELECT * FROM vgatedb.template WHERE GOLD_TEMPLATE_UUID = ?");
		stm.setString(1, goldUUID);
		ResultSet rs = stm.executeQuery();
		while (rs.next()) {
			Template template = new Template();
			template.setGold_UUID(rs.getString("GOLD_TEMPLATE_UUID"));
			template.setUUID(rs.getString("TEMPLATE_UUID"));
			template.setHost_UUID(rs.getString("HOST_UUID"));
			template.setHost_UserName(rs.getString("HOST_USERNAME"));
			template.setHost_IP(rs.getString("HOST_IP"));
			template.setHost_Password(Encryptor.decrypt(rs.getString("HOST_PASSWORD")));
			template.setShared(rs.getBoolean("SHARED"));
			list.add(template);
		}
		return list;
	}
	
	public String findTemplateByHostUUID(String hostUUID, String goldUUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("SELECT TEMPLATE_UUID FROM vgatedb.template WHERE HOST_UUID = ? AND GOLD_TEMPLATE_UUID = ?");
		stm.setString(1, hostUUID);
		stm.setString(2, goldUUID);
		ResultSet rs = stm.executeQuery();
		while(rs.next()) {
			return rs.getString(1);
		}
		return null;
	}
	
	public void insertTemplate(Template template) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("INSERT vgatedb.template VALUES (?, ?, ?, ?, ?, ?, ?)");
		stm.setString(1, template.getUUID());
		stm.setString(2, template.getGold_UUID());
		stm.setString(3, template.getHost_UUID());
		stm.setString(4, template.getHost_IP());
		stm.setString(5, template.getHost_UserName());
		stm.setString(6, Encryptor.encrypt(template.getHost_Password()));
		stm.setBoolean(7, template.isShared());
		stm.executeUpdate();
	}
	
	public void deleteTemplateByGoldTempateUUID(String goldUUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("DELETE FROM vgatedb.template WHERE GOLD_TEMPLATE_UUID = ?");
		stm.setString(1, goldUUID);
		stm.executeUpdate();
	}
	
	public void deleteTemplate(String templateUUID) throws SQLException {
		PreparedStatement stm = this.con.prepareStatement("DELETE FROM vgatedb.template WHERE TEMPLATE_UUID = ?");
		stm.setString(1, templateUUID);
		stm.executeUpdate();
	}

}
