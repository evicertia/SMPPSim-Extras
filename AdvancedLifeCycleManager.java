/****************************************************************************
 * AdvancedLifeCycleManager.java
 *
 * Copyright (C) Evidencias Certificadas, S.L. 2016
 *
 * This file is part of SMPPSim.
 *
 * SMPPSim is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * SMPPSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SMPPSim; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @author Pablo Ruiz
 * Derived from works by martin@seleniumsoftware.com
 ****************************************************************************/

package com.evicertia.SMPPSim;

import com.seleniumsoftware.SMPPSim.LifeCycleManager;
import com.seleniumsoftware.SMPPSim.MessageState;
import com.seleniumsoftware.SMPPSim.SMPPSim;
import com.seleniumsoftware.SMPPSim.Smsc;
import com.seleniumsoftware.SMPPSim.pdu.*;

import java.util.logging.*;

public class AdvancedLifeCycleManager extends LifeCycleManager {

	private static Logger logger = Logger.getLogger("com.evicertia.SMPPSim");

	private Smsc smsc = Smsc.getInstance();

	private int discardThreshold;

	public AdvancedLifeCycleManager() {
		discardThreshold = SMPPSim.getDiscardFromQueueAfter();
		logger.finest("discardThreshold=" + discardThreshold);
	}

        boolean isFailure(byte state) {
                switch (state) {
                case PduConstants.DELIVERED:
                        return false;
                case PduConstants.ACCEPTED:
                        return false;
                default:
                        return true;
                }
        }

        void prepDeliveryReceipt(MessageState m, SubmitSM p) {
                logger.info("Delivery Receipt requested");
                smsc.prepareDeliveryReceipt(p, m.getMessage_id(), m.getState(), 1, 1, m.getErr());
        }

	public MessageState setState(MessageState m) {
		// Should a transition take place at all?
		if (isTerminalState(m.getState()))
			return m;
		byte currentState = m.getState();
		String dest = m.getPdu().getDestination_addr();
		if (dest.substring(0, 1).equals("1")) {
			m.setState(PduConstants.EXPIRED);
			m.setErr(903);
		} else if (dest.substring(0, 1).equals("2")) {
			m.setState(PduConstants.DELETED);
			m.setErr(904);
		} else if (dest.substring(0, 1).equals("3")) {
			m.setState(PduConstants.UNDELIVERABLE);
			m.setErr(901);
		} else if (dest.substring(0, 1).equals("4")) {
			m.setState(PduConstants.ACCEPTED);
			m.setErr(2);
		} else if (dest.substring(0, 1).equals("5")) {
			m.setState(PduConstants.REJECTED);
			m.setErr(902);
		} else {
			m.setState(PduConstants.DELIVERED);
			m.setErr(0);
		}
		if (isTerminalState(m.getState())) {
			m.setFinal_time(System.currentTimeMillis());
			// If delivery receipt requested prepare it....
			SubmitSM p = m.getPdu();
			logger.info("Message:"+p.getSeq_no()+" state="+getStateName(m.getState()));
			if (((p.getRegistered_delivery_flag() & 1) != 0) && currentState != m.getState()) {
				prepDeliveryReceipt(m, p);
			} else {
				if (((p.getRegistered_delivery_flag() & 2) != 0) && currentState != m.getState()) {
					if (isFailure(m.getState())) {
						prepDeliveryReceipt(m, p);
					}
				}
			}
		}
		return m;
	}
}
