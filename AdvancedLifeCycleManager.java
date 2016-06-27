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
		logger.info("Using DeterministicLifeCycleManager");

		if (isTerminalState(m.getState()))
		{
			logger.info("isTerminalState before change state.");
			return m;
		}

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
			logger.info("isTerminalState after change state.");
			m.setFinal_time(System.currentTimeMillis());
			// If delivery receipt requested prepare it....
			SubmitSM p = m.getPdu();
			logger.info("Message:"+p.getSeq_no()+" state="+getStateName(m.getState()));
			logger.info("p.getRegistered_delivery_flag() on dec:  " + p.getRegistered_delivery_flag());
			logger.info("p.getRegistered_delivery_flag() on hex:  " + Integer.toHexString(p.getRegistered_delivery_flag()));
			logger.info("p.getRegistered_delivery_flag() on bin:  " + Integer.toBinaryString(p.getRegistered_delivery_flag()));
			logger.info("currentState:" + currentState );
			logger.info("state:" + m.getState());

			if (IsFlagActivate(p.getRegistered_delivery_flag(),DELIVERY_OUTCOME_SUCCESS_OR_FAILURE,1) && currentState != m.getState()) {
				logger.info("(p.getRegistered_delivery_flag() & 0x0001 ) == 1 && currentState != m.getState()) was true.");
				prepDeliveryReceipt(m, p);
			} else {
				if (IsFlagActivate(p.getRegistered_delivery_flag(),DELIVERY_OUTCOME_FAILURE,2) && currentState != m.getState()) {
					logger.info("((p.getRegistered_delivery_flag() & 0x0010) == 2 && currentState != m.getState()) was true");
					if (isFailure(m.getState())) {
						logger.info("isFailure(m.getState()) is true. ");
						prepDeliveryReceipt(m, p);
					}
				}
			}
		}
		logger.info("Finish Using DeterministicLifeCycleManager");
		return m;
	}


		private int DELIVERY_OUTCOME_SUCCESS_OR_FAILURE = 0x0001;
		private int DELIVERY_OUTCOME_FAILURE = 0x0010;

		private boolean IsFlagActivate(int flag, int maskToCompare, int expectedValue)
		{
			return (flag & maskToCompare) == expectedValue;
		}

		/*
		Info about mask to apply:

		7 6 5 4 3 2 1 0
		x x x x x x 0 0 No MC Delivery Receipt requested (default)
		x x x x x x 0 1 MC Delivery Receipt requested where final delivery outcome is delivery success or failure
		x x x x x x 1 0 MC Delivery Receipt requested where the final delivery outcome is delivery failure.
		x x x x x x 1 1 MC Delivery Receipt requested where the final delivery outcome is success

		x x x x 0 0 x x No recipient SME acknowledgment requested (default)
		x x x x 0 1 x x SME Delivery Acknowledgement requested
		x x x x 1 0 x x SME Manual/User Acknowledgment requested SME originated Acknowledgement (bits 3 and 2)
		x x x x 1 1 x x Both Delivery and Manual/User Acknowledgment requested

		x x x 0 x x x x No Intermediate notification requested (default)
		x x x 1 x x x x Intermediate notification requested

		*/
}
