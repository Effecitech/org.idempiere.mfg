package org.libero.process;

import org.compiere.model.MStorageReservation;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MPPOrderBOMLine;

public class ChangeBOMProduct extends SvrProcess {

	private int pp_order_id;
	private int m_product_id;
	private int m_product_to_id;

	@Override
	protected void prepare() {
		pp_order_id = getRecord_ID();

		for (ProcessInfoParameter param : getParameter()) {
			if (param.getParameterName().equalsIgnoreCase("m_product_id")) {
				m_product_id = param.getParameterAsInt();
			} else if (param.getParameterName().equalsIgnoreCase("m_product_to_id")) {
				m_product_to_id = param.getParameterAsInt();
			}
		}
	}

	@Override
	protected String doIt() throws Exception {
		String trxName = get_TrxName();

		String sql = "SELECT pp_order_bomline_id FROM pp_order_bomline WHERE pp_order_id = ? AND m_product_id = ?";
		int pp_order_bomline_id = DB.getSQLValue(trxName, sql, pp_order_id, m_product_id);

		if (pp_order_bomline_id <= 0) {
			return "Nessuna riga con questo prodotto trovata!";
		}

		MPPOrderBOMLine bomline = new MPPOrderBOMLine(Env.getCtx(), pp_order_bomline_id, trxName);
		MPPOrderBOMLine bomlineTo = new MPPOrderBOMLine(Env.getCtx(), 0, trxName);

		// Copy from original bomline
		PO.copyValues(bomline, bomlineTo);
		bomlineTo.setM_Product_ID(m_product_to_id);
		bomlineTo.saveEx(trxName);

		// Update reserved qty
		MStorageReservation.add(Env.getCtx(), bomlineTo.getM_Warehouse_ID(), bomlineTo.getM_Product_ID(),
				bomlineTo.getM_AttributeSetInstance_ID(), bomlineTo.getQtyRequired(), true, trxName);

		sql = "DELETE FROM pp_mrp WHERE pp_order_bomline_id = ?";
		DB.executeUpdateEx(sql, new Object[] { bomline.getPP_Order_BOMLine_ID()}, trxName);
		
		// Delete old bomline
		bomline.deleteEx(true, trxName);

		return null;
	}

}
