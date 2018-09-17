package nantian.sendmsg.task;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import mobset.smsSDK;
import mobset.str_SendMsg;
import sendmsg.ISms;
import sendmsg.SmsUtil;

public class SmsClient extends Task implements ISms {
	private Logger log = Logger.getLogger("Scheduler");
	private boolean serverConncet = false;
	private static smsSDK sdk = null;
	private String sendType;
	private SmsUtil smsUtil = new SmsUtil();
	private String host;
	private String corpID;
	private String loginName;
	private String passWord;

	private String machine;
	private int port;
	private int timeOut = 2;
	private String message = null;
	private String response = null;
	private String phone;

	public String sendMsg(String mobile, String content) {
		if ("1".equals(this.sendType.trim())) {
			return sendMsgXml(mobile, content);
		}
		String strRet = "";
		boolean retFlag = false;
		try {
			int iRet = 0;
			int retry = 0;
			if (!connectServer())
				return null;
			while (true) {
				str_SendMsg[] sendMsg = new str_SendMsg[1];
				sendMsg[0] = new str_SendMsg();
				sendMsg[0].strMobile = mobile;
				sendMsg[0].strMsg = content;
				int result = sdk.Sms_Send(sendMsg, 1);

				iRet = sendMsg[0].iSmsID;
				if ((result != -1) || (retry >= 2))
					break;
				retry++;
				this.serverConncet = false;
				retFlag = true;
				Thread.sleep(3000L);
			}

			strRet = Integer.toString(iRet);
		} catch (Exception e) {
			this.serverConncet = false;
			strRet = null;
		} finally {
			if (retFlag) {
				this.serverConncet = false;
				disconnectServer();
			}
		}

		return strRet;
	}

	public int sendStat(String msgID) {
		int ret = -1;
		boolean retFlag = false;
		try {
			if (!connectServer()) {
				return ret;
			}
			ret = sdk.Sms_Status(Integer.parseInt(msgID));
			if (ret == -1) {
				this.serverConncet = false;
				retFlag = true;
			}
		} catch (Exception e) {
			this.serverConncet = false;
		} finally {
			if (retFlag) {
				this.serverConncet = false;
				disconnectServer();
			}
		}
		return statDefine(ret);
	}

	private boolean connectServer() {
		try {
			if (this.serverConncet) {
				return this.serverConncet;
			}
			sdk = new smsSDK();

			int iRet = sdk.Sms_Connect(this.host,
					Integer.parseInt(this.corpID), this.loginName,
					this.passWord, this.timeOut);

			if (iRet == 0) {
				iRet = sdk.Sms_KYSms();
				if (iRet >= 0) {
					this.serverConncet = true;
				} else
					disconnectServer();
			} else {
				disconnectServer();
			}
		} catch (Exception e) {
			disconnectServer();
		}
		return this.serverConncet;
	}

	public void disconnectServer() {
		try {
			if (sdk != null) {
				sdk.Sms_DisConnect();
			}
			sdk = null;
		} catch (Exception localException) {
		}
	}

	private int statDefine(int statID) {
		int ret = -1;

		if ((statID != -5) && (statID != -4) && (statID != -3)
				&& (statID != -2) && (statID != -1)) {
			if (statID == 0)
				ret = 0;
			else if (statID == 1)
				ret = 0;
			else if ((statID != 2) && (statID != 3) && (statID != 4)
					&& (statID != 5) && (statID != 6) && (statID != 7))
				if (statID == 10) {
					ret = 1;
				} else if (statID != 11) {
					if (statID == 12)
						ret = 1;
					else if (statID == 13)
						;
				}
		}
		return ret;
	}

	public String sendMsgXml(String mobile, String content) {
		String strRet = "";
		String revMsg = "";
		try {
			int retry = 0;
			String ret_code;
			while (true) {
				String sendXml = this.smsUtil.createXmlMsg(mobile, content);
				revMsg = this.smsUtil.sendMessage(machine, port, this.timeOut,
						sendXml);
				if (revMsg.length() > 6) {
					revMsg = revMsg.substring(6, revMsg.lastIndexOf(">") + 1);
				}
				ret_code = this.smsUtil.readXmlString(revMsg, "ret_code");
				if (("00000".equals(ret_code)) || (retry >= 2))
					break;
				retry++;
				Thread.sleep(3000L);
			}

			if ("00000".equals(ret_code)) {
				String send_req_seq = this.smsUtil.readXmlString(revMsg,
						"send_req_seq");
				strRet = strRet + Integer.parseInt(send_req_seq.substring(3));
			} else {
				strRet = null;
			}
		} catch (Exception e) {
			strRet = null;
		}

		return strRet;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String Host) {
		this.host = Host;
	}

	public String getCorpID() {
		return this.corpID;
	}

	public void setCorpID(String corpid) {
		this.corpID = corpid;
	}

	public String getLoginName() {
		return this.loginName;
	}

	public void setLoginName(String loginname) {
		this.loginName = loginname;
	}

	public String getPassWord() {
		return this.passWord;
	}

	public void setPassWord(String password) {
		this.passWord = password;
	}

	public String getSendType() {
		return this.sendType;
	}

	public void setSendType(String sendType) {
		this.sendType = sendType;
	}

	/**
	 * @return 获得 machine
	 */
	public String getMachine() {
		return machine;
	}

	/**
	 * @param machine
	 *            设置 machine
	 */
	public void setMachine(String machine) {
		this.machine = machine;
	}

	/**
	 * @return 获得 port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            设置 port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return 获得 timeOut
	 */
	public int getTimeOut() {
		return timeOut;
	}

	/**
	 * @param timeOut
	 *            设置 timeOut
	 */
	public void setTimeOut(int timeOut) {
		this.timeOut = timeOut;
	}

	/**
	 * @return 获得 message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            设置 message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return 获得 response
	 */
	public String getResponse() {
		return response;
	}

	/**
	 * @param response
	 *            设置 response
	 */
	public void setResponse(String response) {
		this.response = response;
	}

	/**
	 * @return 获得 phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @param phone
	 *            设置 phone
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	public void execute() {
		SmsClient client = new SmsClient();
		if (phone != null && !"".equals(phone)) {
			throw new BuildException("phone  is null");
		}
		if (message != null && !"".equals(message)) {
			throw new BuildException("message  is null");
		}
		if (machine != null && !"".equals(machine)) {
			throw new BuildException("machine's ip  is null");
		}
		String result;
		try {
			result = client.sendMsgXml(phone, message);
			if ((result != null) && (!"".equals(result))
					&& ("1".equals(result))) {
				getProject().setUserProperty(this.response, "success");
			} else {
				getProject().setUserProperty(this.response, "fail");
			}
		} catch (Exception e) {
			getProject().setUserProperty(this.response, "fail");
			log.error(e.getMessage());
		}
	}
}