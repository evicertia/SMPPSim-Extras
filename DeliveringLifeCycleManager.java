/****************************************************************************
 * DeliveringLifeCycleManager.java
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

public class DeliveringLifeCycleManager extends LifeCycleManager {

	private static Logger logger = Logger.getLogger("com.evicertia.SMPPSim");

	private Smsc smsc = Smsc.getInstance();

	private int discardThreshold;

	private int DELIVERY_OUTCOME_SUCCESS_OR_FAILURE = 0x0001;

	private int DELIVERY_OUTCOME_FAILURE = 0x0010;

	public DeliveringLifeCycleManager() {
		discardThreshold = SMPPSim.getDiscardFromQueueAfter();
		logger.finest("discardThreshold=" + discardThreshold);
	}

	private boolean HasFlag(int delivery_flag, int flag)
	{
		return (delivery_flag & flag) == flag;
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
		logger.info("Using DeliveringLifeCycleManager...");

		if (isTerminalState(m.getState()))
		{
			logger.info("isTerminalState before change state.");
			return m;
		}

		byte currentState = m.getState();
		m.setState(PduConstants.DELIVERED);
		m.setErr(0);

		if (isTerminalState(m.getState())) {
			logger.info("isTerminalState after change state.");
			m.setFinal_time(System.currentTimeMillis());
			// If delivery receipt requested prepare it....
			SubmitSM p = m.getPdu();
			logger.info("Message:"+p.getSeq_no()+". Message State="+getStateName(m.getState()) + ". CurrentState:" + currentState);
			logger.info("p.getRegistered_delivery_flag() on hex:  " + Integer.toHexString(p.getRegistered_delivery_flag()));

			if (HasFlag(p.getRegistered_delivery_flag(), DELIVERY_OUTCOME_SUCCESS_OR_FAILURE) && currentState != m.getState()) {
				prepDeliveryReceipt(m, p);
			} else {
				if (HasFlag(p.getRegistered_delivery_flag(), DELIVERY_OUTCOME_FAILURE) && currentState != m.getState()) {
					if (isFailure(m.getState())) {
						prepDeliveryReceipt(m, p);
					}
				}
			}
		}

		return m;
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
