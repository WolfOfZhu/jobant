package nantian.order.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.springframework.jdbc.core.JdbcTemplate;

import nantian.order.util.OrderHander;
import nantian.order.util.OrderUtil;

/**
 **************************************************************** 
 * 公司名称 :广州南天电脑系统有限公司 系统名称 :checkxml 功能描述 :TODO 业务描述 : 作 者 名 :@sky_zhu 开发日期
 * :2018年1月8日 下午5:55:18
 ***************************************************************
 * 修改日期 : 修 改 者 : 修改内容 :
 ***************************************************************
 */
public class OrderTask extends Task {
	private final Logger log = Logger.getLogger("Scheduler");
	private String datasource = null;// 数据库连接
	private static JdbcTemplate conn;

	/**
	 * @param datasource
	 *            设置 datasource
	 */
	public void setDatasource(String datasource) {
		this.datasource = datasource;
	}

	public void execute() {
		orderMain();
	}

	public void orderMain() {
		if (!OrderUtil.isNotEmpty(getDBConnection())) {
			throw new BuildException("DB connection fail");
		} else {
			conn = getDBConnection();// 连接数据库
		}
		long start = System.currentTimeMillis();
		List<Map<String, String>> devChange = null;
		ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			String devSqlChange = "SELECT G.dev_code as dev_code,substr(g.dev_code,0,1) as dev_class,nvl(jg.branch_name,'广东省农村信用社联合社[全省汇总(不包括省清算中心)]') as dev_jg"
					+ " FROM DEV_SMSG_SNX G "
					+ " JOIN DEV_BMSG GG "
					+ " ON GG.DEV_CODE=G.DEV_CODE "
					+ "left join branch_msg jg on jg.branch_code=gg.dev_bank "
					+ " where gg.run_flag='1' and nvl(g.devicetype,0)='0'  and substr(g.last_check_time,0,8)=to_char(sysdate,'yyyymmdd') ";
			devChange = conn.queryForList(devSqlChange);
			if (devChange.size() == 0) {
				return;
			} else if (devChange.size() > 500) {
				for (int i = 0; i < 20; i++) {
					executorService.execute(new OrderHander(assignList(
							devChange, 20, i), conn));
				}
				executorService.shutdown();
			} else {
				executorService.execute(new OrderHander(assignList(devChange,
						1, 0), conn));
				executorService.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		log.info("order cost time：" + (end - start));
	}

	// 按数据量开启线程 500-3
	public static List<Map<String, String>> assignList(
			List<Map<String, String>> tmp, int num, int desc) {
		List<List<Map<String, String>>> result = new ArrayList<List<Map<String, String>>>();
		int rem = tmp.size() % num;
		int number = tmp.size() / num;
		int offset = 0;
		for (int i = 0; i < num; i++) {
			List<Map<String, String>> value = null;
			if (rem > 0) {
				value = tmp.subList(i * number + offset, (i + 1) * number
						+ offset + 1);
				rem--;
				offset++;
			} else {
				value = tmp.subList(i * number + offset, (i + 1) * number
						+ offset);
			}
			result.add(value);
		}
		return result.get(desc);
	}

	// 数据库连接
	private JdbcTemplate getDBConnection() throws BuildException {
		try {
			if ((OrderUtil.isNotEmpty(this.datasource))
					&& (getProject().getReference(this.datasource) != null)) {
				return new JdbcTemplate((DataSource) getProject().getReference(
						this.datasource));
			}
			return null;
		} catch (Exception e) {
			log(e.getMessage(), 0);
			throw new BuildException(e.getMessage());
		}
	}
}
