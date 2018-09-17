package nantian.order.util;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 **************************************************************** 
 * 公司名称 :广州南天电脑系统有限公司 
 * 系统名称 :orderjob 
 * 功能描述 :生成关闭工单
 * 业务描述 : 工单监控
 * 作 者 名 :@sky_zhu 
 * 开发日期:2018年1月8日 下午5:41:02
 ***************************************************************
 * 修改日期 :
 * 修 改 者 : 
 * 修改内容 :
 ***************************************************************
 */
public class OrderHander  implements Runnable {
	private final Logger log = Logger.getLogger("Scheduler");
	private  final List<Map<String, String>> change;//当前所有投产的设备
	private final JdbcTemplate conn;
	public OrderHander(List<Map<String, String>> change,JdbcTemplate conn) {
		this.change = change;
		this.conn=conn;
	}

	@Override
	public void run() {
		try {
			createCaseJob(change);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createCaseJob(List<Map<String, String>> tmp) throws Exception {
		/**
		 * 状态改变的设备
		 */

		List<Map<String, String>> devChange = tmp;
		for (Map<String, String> change : devChange) {
			String devCode = change.get("DEV_CODE");
			String devClass = change.get("DEV_CLASS");
			String devJgName = change.get("DEV_JG");
//			log.info("第一步");
			//当前的设备状态的值
			Map<String, String> snxMap = null;
			//部件值
			List wspartsVar = null;
			// 记录每个设备不同状态和状态值
			Map<String, String> diffstatus = new HashMap<String, String>();

			String sqlSnx = "select * from dev_smsg_snx where dev_code=?";
			String sqlWsparts = "select b.dev_code, a.ptsid, a.statval, a.casename, a.casetype "
					+ "from ws_case a "
					+ "inner join (select a.dev_code, "
					+ "(case when (select max(1) from ws_case c where b.dev_bank = c.branch_code and c.dev_class = substr(a.dev_code, 0, 1)) = 1 then b.dev_bank else '00001' end) jg "
					+ "from dev_smsg_snx a, dev_bmsg b "
					+ "where a.dev_code = b.dev_code) b "
					+ "on a.dev_class = substr(b.dev_code, 0, 1) "
					+ "and a.branch_code = b.jg and a.startflag='0' and b.dev_code=?";
			snxMap = new HashMap<String, String>();
			wspartsVar = new ArrayList();
			snxMap = conn.queryForMap(sqlSnx,
					new String[] { devCode });
			wspartsVar = conn.queryForList(sqlWsparts,
					new String[] { devCode });
			for (int i = 0; i < (wspartsVar.size() == 0 ? 0 : wspartsVar.size()); i++) {
				Map<String, String> ptsidMap = (Map<String, String>) wspartsVar
						.get(i);
				// 事件状态
				String ptsid = (String) ptsidMap.get("PTSID");
				// 事件状态值
				String value = (String) ptsidMap.get("STATVAL");
				String caseType = (String) ptsidMap.get("CASETYPE");
				if (OrderUtil.isNotEmpty(snxMap) && OrderUtil.isNotEmpty(ptsid)) {
					String snxStatus = snxMap.get(ptsid.toUpperCase());
					if (OrderUtil.isNotEmpty(snxStatus)
							&& snxStatus.equals(value)) {
						diffstatus.put(ptsid, caseType);
					} else {
						// 关闭工单
						closeOrder(caseType, devCode);
						log.info("closeOrder-devcode:"+devCode+"-ptsid:"+ptsid+"-status:"+snxStatus);
					}
				}
			}
			// 判断改变的状态值，生成工单
			if (OrderUtil.isNotEmpty(diffstatus)) {
				getOrderOrNot(devCode, devClass,devJgName,diffstatus);
			}
		}
	}

	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2017年8月10日 下午2:16:08
	 * @serverCode 服务代码
	 * @serverComment 服务注解--工单主服务
	 *
	 * @param dev_code
	 *            设备号
	 * @param dev_class
	 *            设备类型
	 * @param dev_jg
	 *            设备所属机构
	 * @param ptsid
	 *            设备的部件
	 */
	@SuppressWarnings("unchecked")
	private void getOrderOrNot(String devCode, String devClass,String jgName,
			Map<String, String> diffStatus) throws Exception {
//		log.info("第二步");
		Map<String, String> orderOfptisd = new HashMap<String, String>();
		if (OrderUtil.isNotEmpty(diffStatus)) {
			for (String ptsid : diffStatus.keySet()) {
				String caseType = diffStatus.get(ptsid);
				//判断工单是否是否存在
				List<Map<String, String>> wd = null;
				String sqlWd = "select a.casetype,a.serno,a.stepdate1,a.steptime1,a.casestate, a.startflag from wd_casestart a  "
						+ "where a.casetype=? and a.untid=? and a.casestate !='8' ";
				wd = conn.queryForList(sqlWd,
						new String[] { caseType, devCode });
				if (OrderUtil.isNotEmpty(wd)) {
					//则判断此工单创建的时间属于发信息去哪个范围的人
					for (Map<String, String> tmp : wd) {
						overTime(tmp.get("CASETYPE"), tmp.get("STEPDATE1")
								+ tmp.get("STEPTIME1"), devCode,jgName,
								tmp.get("SERNO"));
					}
				} else {
					// 需创建工单casestate='11'待处理
					String sql = "insert into wd_casestart(casetype,serno,untid,userid, stepdate1,steptime1, casestate,startflag)values"
							+ "(?,seq_wd_casestart.nextval,?,?,?,?,?,?)";
					conn.update(
							sql,
							new String[] {
									caseType,
									devCode,
									"",
									new SimpleDateFormat("yyyyMMdd")
											.format(new Date()),
									new SimpleDateFormat("HHmmss")
											.format(new Date()), "11", "0" });
					log.info("devCode:"+devCode+" order producted");

				}

			}
		}

	}

	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2017年8月10日 上午11:04:59
	 * @serverCode 服务代码
	 * @serverComment 服务注解:处理工单-关闭工单
	 *
	 * @param serno
	 *            工单序列号
	 */
	public void closeOrder(String caseType, String devNo) {
//		log.info("第三步");
		//查询工单开始与当前时间的秒数差
		try {
			String sqlWd = "select a.casetype,a.serno,a.casestate, a.startflag from wd_casestart a "
					+ "where a.casetype=? and a.untid=? and a.casestate !='8'";
			List<Map<String, String>> wd = null;
			wd = conn.queryForList(sqlWd,
					new String[] { caseType, devNo });
			if (!OrderUtil.isNotEmpty(wd)) {
				return;
			}

			for (Map<String, String> map : wd) {
				String serno = map.get("SERNO");
				log.info("关闭工单完成");
				String sql = "select ceil(（(to_date(?,'yyyymmddHH24miss')-to_date(a.stepdate1||a.steptime1,'yyyymmddHH24miss'))*24*60*60）)||'' as difftime from (select stepdate1,steptime1 from wd_casestart where serno=?)a ";
				Map<String, String> difftime = conn.queryForMap(
						sql,
						new String[] {
								new SimpleDateFormat("yyyyMMdd")
										.format(new Date())
										+ new SimpleDateFormat("HHmmss")
												.format(new Date()), serno });
				// 处理工单结束casestate='8'
				conn
						.update("update Wd_casestart set stepdate5=?,steptime5=?,finishtime=?,casestate='8' where serno=?",
								new String[] {
										new SimpleDateFormat("yyyyMMdd")
												.format(new Date()),
										new SimpleDateFormat("HHmmss")
												.format(new Date()),
										difftime.get("DIFFTIME"), serno });
				// 转到历史表
				conn
						.update("insert into wl_casestart  select * from wd_casestart where serno = ?",
								new String[] { serno });
				//判断是督办工单，更新督办状态
				List wltmp = new ArrayList();
				wltmp = conn
						.queryForList(
								"select casestate from Wd_casestart where casestate='27' and serno=?",
								new String[] { serno });
				if (OrderUtil.isNotEmpty(wltmp)) {
					conn.update(
							"update wlevent set eventstate='4'  where serno=?",
							new Object[] { serno });
				}
				//结束工单，按工单号清除工单表数据
				conn.update(
						"delete from  wd_casestart  where serno= ? ",
						new String[] { serno });
				log.info("结束工单！按工单号清除工单表数据");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 超时设置
	 * 
	 * @param caseType
	 *            事件类型
	 * @param startTime
	 *            工单开始的时间
	 * @param devcode
	 *            设备编号
	 * @param serno
	 *            工单序列号
	 */
	public void overTime(String caseType, String startTime, String devcode,String jgName,
			String serno) throws Exception {
		/**
		 * 先判断自动回复时间，若超过自动回复的时间，若在服务时间内，立刻发送短信到第一联系人（若没有就不发）；
		 * 超过第一联系人的时间还没回复正常，则发送短信到第二联系人，同理，发到第三联系人 超时配置时间
		 */
		List<Map<String, String>> timeoutList = null;
		timeoutList = conn
				.queryForList(
						"select  wc.casename, wt.waittime, wt.managertime, wt.sendwaittime1, wt.sendwaittime2, wt.sendwaittime3 "
								+ "from ws_case wc "
								+ "left join ws_timeout wt on wt.casetype=wc.casetype "
								+ "where  wc.startflag = '0' and wc.casetype = ?",
						new Object[] { caseType });
		/**
		 * 判断是否超过超时时间
		 */
		if (!OrderUtil.isNotEmpty(timeoutList)) {
			return;
		}
		for (Map<String, String> timeoutmap : timeoutList) {
			/**
			 * 等待自动回复的时间
			 */
			String waittime = timeoutmap.get("WAITTIME") == null ? "20"
					: OrderUtil.bigToString(timeoutmap.get("WAITTIME"));
			/**
			 * 管理员延时
			 */
			String sendwaittime = timeoutmap.get("MANAGERTIME") == null ? "40"
					: OrderUtil.bigToString(timeoutmap.get("MANAGERTIME"));
			/**
			 * 三级行延时
			 */
			String sendwaittime1 = timeoutmap.get("SENDWAITTIME1") == null ? "60"
					: OrderUtil.bigToString(timeoutmap.get("SENDWAITTIME1"));
			/**
			 * 二级行延时
			 */
			String sendwaittime2 = timeoutmap.get("SENDWAITTIME2") == null ? "60"
					: OrderUtil.bigToString(timeoutmap.get("SENDWAITTIME2"));
			/**
			 * 一级行延时
			 */
			String sendwaittime3 = timeoutmap.get("SENDWAITTIME3") == null ? "60"
					: OrderUtil.bigToString(timeoutmap.get("SENDWAITTIME3"));
			/**
			 * 事件名称
			 */
			String casename = timeoutmap.get("CASENAME");
			/**
			 * 三级行延时所需时间
			 */
			String thirdDelay = (Integer.parseInt(sendwaittime) + Integer
					.parseInt(waittime)) + "";

			String secodeDelay = (Integer.parseInt(sendwaittime1)
					+ Integer.parseInt(sendwaittime) + Integer
						.parseInt(waittime)) + "";		
			String firstDelay = (Integer.parseInt(sendwaittime2)
					+ Integer.parseInt(sendwaittime1)
					+ Integer.parseInt(sendwaittime) + Integer
						.parseInt(waittime)) + "";
			// 督办时间
			String perDelay = (Integer.parseInt(sendwaittime3)
					+ Integer.parseInt(sendwaittime2)
					+ Integer.parseInt(sendwaittime1)
					+ Integer.parseInt(sendwaittime) + Integer
						.parseInt(waittime)) + "";
			if (OrderUtil.timeDiff(startTime, waittime)) {
				/**
				 * 工单和短信的具体信息
				 */
				Map<String, String> orderMsg = new HashMap<String, String>();
				orderMsg.put("serno", serno);
				orderMsg.put("devcode", devcode);
				orderMsg.put("casename", casename);
				orderMsg.put("jgname", jgName);
				orderMsg.put("caseType", caseType);
				/**
				 * 服务时间,若没配置，默认0点到24点
				 */
				Map<String, String> alarmtime = new HashMap();
				String sql = "select wsa.starttime,wsa.endtime from ws_case wsc left join ws_alarmtime wsa on wsc.casetype = wsa.casetype where wsc.startflag = '0' and wsc.casetype=?";
				alarmtime = conn.queryForMap(sql,
						new String[] { caseType });

				if (OrderUtil.isNotEmpty(alarmtime)) {
					
					String starttime = alarmtime.get("STARTTIME") == null ? "000001"
							: alarmtime.get("STARTTIME");
					String endtime = alarmtime.get("ENDTIME") == null ? "235959"
							: alarmtime.get("ENDTIME");
					/**
					 * 在服务时间内发送短信给第一联系人
					 */
					if (OrderUtil.timeSecond(starttime) < OrderUtil
							.timeSecond(startTime.substring(8))
							&& OrderUtil.timeSecond(startTime.substring(8)) < OrderUtil
									.timeSecond(endtime)
							&& !OrderUtil.timeDiff(startTime, thirdDelay)) {

						orderMsg.put("waitedTime", sendwaittime);
						orderMsg.put("type", "1");
						getManager(orderMsg);
						log.info("发信息给管理员");
					} else if (OrderUtil.timeDiff(startTime, perDelay)) {

						List tmp = new ArrayList();
						tmp = conn
								.queryForList(
										"select casestate from Wd_casestart where serno=? and casestate='27'",
										new Object[] { serno });
						if (!OrderUtil.isNotEmpty(tmp)) {
							tmp = conn.queryForList(
									"select * from wlevent where serno=?",
									new Object[] { serno });
							if (!OrderUtil.isNotEmpty(tmp)) {
								log.info("变为督办工单");
								int num = conn
										.update("insert into wlevent(eventstate,untid,casetype,serno,startdate,starttime) values('1',?,?,?,?,?)",
												new Object[] {
														devcode,
														caseType,
														serno,
														startTime.substring(0,
																8),
														startTime.substring(8) });
								// casestate=27为督办工单
								if (num > 0) {
									int success = conn
											.update("update Wd_casestart set casestate=?, processinstanceid = ? where serno=?",
													new Object[] { "27", "27",
															serno });
									if (success > 0) {
										return;
									}
									throw new Exception("更新工单状态出错！");
								}
							} else {
								log.info("已经变为督办工单");
							}
						}
					} else if (OrderUtil.timeDiff(startTime, thirdDelay)
							&& !OrderUtil.timeDiff(startTime, secodeDelay)) {
						orderMsg.put("waitedTime", sendwaittime3);
						orderMsg.put("type", "3");
						getManager(orderMsg);
						log.info("发信息给三级行");
					} else if (OrderUtil.timeDiff(startTime, secodeDelay)
							&& !OrderUtil.timeDiff(startTime, firstDelay)) {
						orderMsg.put("waitedTime", sendwaittime2);
						orderMsg.put("type", "4");
						getManager(orderMsg);
						log.info("发信息给二级行");
					} else if (OrderUtil.timeDiff(startTime, thirdDelay)
							&& !OrderUtil.timeDiff(startTime, perDelay)) {
						orderMsg.put("waitedTime", sendwaittime1);
						orderMsg.put("type", "5");
						getManager(orderMsg);
					}
				}
			}

		}
	}

	/**
	 * 记录发过信息的联系人，先判断是否当前发过信息，若没有就发，有的话，下一步。
	 * 
	 * @param orderMsg
	 *            工单信息
	 */
	public void getManager(Map<String, String> orderMsg) throws Exception {
		String serno = orderMsg.get("serno");
		String devcode = orderMsg.get("devcode");
		String type = orderMsg.get("type");
		String casename = orderMsg.get("casename");
		String waitedTime = orderMsg.get("waitedTime");
		String jgname = orderMsg.get("jgname");
		String caseType = orderMsg.get("caseType");
		/**
		 * 查询工单的发过短信的状态
		 */
		String sql = "";
		Map<String, String> casestateMap = new HashMap<String, String>();
		sql = "select casestate from wd_casestart where serno=?";
		casestateMap = conn.queryForMap(sql,
				new Object[] { serno });
		/**
		 * 超时设置的时间
		 */
		for (String key : casestateMap.keySet()) {

			String casestate = casestateMap.get(key);
			if (OrderUtil.isNotEmpty(casestate)) {
				/**
				 * 判断是否管理员发过信息并且时间符合和人和号码存在就发信息
				 */
				if (!"22".equals(casestate) && "1".equals(type)) {
					/**
					 * 判断是否管理员发过信息
					 */
					insertMessage(serno, devcode, "22", type, casename,
							waitedTime, jgname, caseType);
				} else if (!"24".equals(casestate) && "3".equals(type)) {
					/**
					 * 判断是否三级行发过信息 并且时间符合和人和号码存在就发信息
					 */
					insertMessage(serno, devcode, "24", type, casename,
							waitedTime, jgname, caseType);
				} else if (!"25".equals(casestate) && "4".equals(type)) {

					/**
					 * 判断是否二级行发过信息并且时间符合和人和号码存在就发信息
					 */
					insertMessage(serno, devcode, "25", type, casename,
							waitedTime, jgname, caseType);
				} else if (!"26".equals(casestate) && "5".equals(type)) {

					/**
					 * 判断是否一级行发过信息并且时间符合和人和号码存在就发信息
					 */
					insertMessage(serno, devcode, "26", type, casename,
							waitedTime, jgname, caseType);
				}
				/**
				 * 当一级行信息发过之后超过处理时间，设备还没变正常，变为督办工单
				 */
			} else {
				return;
			}
		}

	}

	/**
	 * 把信息插入短信表更新工单发信息的状态
	 * 
	 * @param serno
	 *            工单的序列号
	 * @param devcode
	 *            设备号
	 * @param caseStatus
	 *            工单状态
	 * @param type
	 *            联系人类型
	 * @param casename
	 *            事件名字
	 * @param waitedTime
	 *            联系人延时处理时间
	 * @param jgname
	 *            机构名字
	 * @param caseType
	 *            事件类型
	 * @throws Exception
	 */
	public void insertMessage(String serno, String devcode, String caseStatus,
			String type, String casename, String waitedTime, String jgname,
			String caseType) throws Exception {

		Map<String, String> phandper = msgDetail(devcode, type);
		String phone = "";
		String managerName = "";
		int i = 1;// 控制下面的for进入循环的次数
		int num = 0;// 插入短信表是否成功
		for (String key : phandper.keySet()) {
			managerName = key;
			phone = phandper.get(key);
			// 根据type设置设备联系人级别，eova_dict where object='case_query' and
			// field='method';字典表
			// 信息内容
			String devlevel = type;
			if ("1".equals(type) && i == 2) {
				devlevel = "2";
			} else if ("3".equals(type) && i == 2) {
				devlevel = "6";
			} else if ("4".equals(type) && i == 2) {
				devlevel = "7";
			} else if ("5".equals(type) && i == 2) {
				devlevel = "8";
			}
			i++;
			String det = "机构：" + jgname + "\n设备号：" + devcode + "\n故障信息："+casename
					+ "\n麻烦在" + waitedTime + "分钟内修复";
			Object[] message = new Object[] { devcode, serno, managerName,
					phone, det, caseType, devlevel, "2", type, "case",
					new SimpleDateFormat("yyyyMMdd").format(new Date()),
					new SimpleDateFormat("HHmmss").format(new Date()) };
			try {
				num = conn
						.update("insert into w_msgej(id,untid,msgserno,userid,usrmobil,msgtail,worknum,method,flag,grade,msg_id, create_time, update_time,senddate,sendtime) values (seq_w_msgej.nextval,?,?,?,?,?,?,?,?,?,?,to_char(sysdate,'yyyymmddhh24miss'),to_char(sysdate,'yyyymmddhh24miss'),?,?) ",
								message);
			} catch (Exception e) {
				throw new Exception("插入短信流水表出错！");
			}
		}
		if (num > 0) {
			int success = conn
					.update("update Wd_casestart set casestate=?, processinstanceid = ? where serno=?",
							new Object[] { caseStatus, caseStatus, serno });
			if (success > 0) {
				return;
			}
			throw new Exception("更新工单状态出错！");
		}
	}

	/**
	 * 发送短信的联系人和发送的信息内容
	 * 
	 * @param devCode
	 *            设备编号
	 * @param managerType
	 *            发送人的类型（第一联系人，第二联系人）
	 * @return 若不存在，返回null
	 */
	public Map<String, String> msgDetail(String devCode, String managerType) {
		List<Map<String, String>> phoneList = new ArrayList<Map<String, String>>();
		phoneList = conn
				.queryForList(
						"select mobile,manager,manager_name from dev_manager_msg where dev_Code=? ",
						new Object[] { devCode });
		if(!OrderUtil.isNotEmpty(phoneList)){
			phoneList = conn
					.queryForList(
							"select mobile,manager as manager_name,'第一联系人' as manager from dev_bmsg where dev_code=?",
							new Object[] { devCode });
		}
		Map<String, String> detail = new HashMap<String, String>();
		for (Map<String, String> phoneMap : phoneList) {
			if (OrderUtil.isNotEmpty(phoneMap)) {
				String mobile = phoneMap.get("MOBILE") == null ? "" : ""
						+ phoneMap.get("MOBILE");
				String managerLevel = phoneMap.get("MANAGER") == null ? "" : ""
						+ phoneMap.get("MANAGER");
				String managerName = phoneMap.get("MANAGER_NAME") == null ? ""
						: phoneMap.get("MANAGER_NAME").toString();
				if (OrderUtil.isNotEmpty(mobile)
						&& OrderUtil.isNotEmpty(managerLevel)
						&& OrderUtil.isNotEmpty(managerName)) {
					if (("第一联系人".equals(managerLevel.trim()))
							&& "1".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("第二联系人".equals(managerLevel.trim())
							&& "1".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("三级行".equals(managerLevel.trim())
							&& "3".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("manu_3".equals(managerLevel.trim())
							&& "3".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("二级行".equals(managerLevel.trim())
							&& "4".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("manu_2".equals(managerLevel.trim())
							&& "4".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("一级行".equals(managerLevel.trim())
							&& "5".equals(managerType)) {
						detail.put(managerName, mobile);
					} else if ("manu_1".equals(managerLevel.trim())
							&& "5".equals(managerType)) {
						detail.put(managerName, mobile);
					}
				}
			}
		}
		return detail;
	}
}
