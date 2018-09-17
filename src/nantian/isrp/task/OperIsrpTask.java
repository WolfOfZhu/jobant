package nantian.isrp.task;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import mp.isrpapi.IsrpClientApi;
import mp.isrpapi.isrpApiImpl.IsrpClientApiHttpImpl;
import mp.model.FileElementModel;
import mp.model.InitModel;
import mp.model.IsrpResult;
import mp.model.ResultModel;
import mp.model.TxnElementModel;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 **************************************************************** 
 * 公司名称 :广州南天电脑系统有限公司 系统名称 :nantianjob 功能描述 :上传文件到内容平台，更新数据库 业务描述 :
 * 根目录的三级目录是日期目录，返回的结果response--success，fail 作 者 名 :@sky_zhu 开发日期 :2018年4月9日
 * 上午11:01:53
 ***************************************************************
 * 修改日期 :登录，登出，新增 修 改 者 : 修改内容 :
 ***************************************************************
 */
public class OperIsrpTask extends Task {
	private Logger log = Logger.getLogger("Scheduler");
	private String datasource = null;// 数据库连接

	private static JdbcTemplate conn;
	private String clientIp = null;// 连接内容平台的ip
	private String clientPort = null;// 端口号
	private String clientName = null;// 服务名
	private String clientAppid = null;// 服务id
	private String clientUserid = null;// 用户id
	private String clientPwd = null;// 用户密码
	private String clientjgh = null;// 机构号
	private String clientfrh = null;// 法人代码
	private String clientgyh = null;// 法人代码
	private String filepath = null;// 上传的文件全局路径必填
	private String filetype = null;// 上传的文件类型,若填空，默认pdf

	private String response = null;// 上传成功，更新到数据库成功

	String today = new SimpleDateFormat("yyyyMMdd").format(new Date());// 当天日期

	public void execute() {
		isrpMain();
	}

	// api初始化--》登录---》批次新增--记录到数据库---》删除文件---》登出
	public void isrpMain() {
		InitModel tokenModel;// 共用的token对象
		if (!objIsNotNull(clientIp) && !objIsNotNull(clientPort)) {
			throw new BuildException("ip or port is null");
		}
		if (!objIsNotNull(filetype)) {
			filetype = "pdf";
		}
		if (!objIsNotNull(filepath)) {
			throw new BuildException("upload's filepath is null");
		}
		if (!objIsNotNull(clientAppid)) {
			throw new BuildException("clientAppid is null");
		}
		if (!objIsNotNull(getDBConnection())) {
			throw new BuildException("DB connection fail");
		} else {
			conn = getDBConnection();// 连接数据库
		}
		// api初始化
		IsrpClientApi isrpClientApi = new IsrpClientApiHttpImpl(clientIp,
				clientPort, clientName, "");
		// 登录
		boolean inflag = false;// 登录标识
		IsrpResult loginrs = isrpClientApi.loginIsrp(clientUserid, clientPwd,
				clientjgh, clientfrh, clientAppid, clientgyh);
		if ("0000".equals(loginrs.getReasonId().trim())) {
			inflag = true;
			tokenModel = (InitModel) loginrs.getObj();
			log.info("login success--token:" + tokenModel.getToken());
		} else {
			log.info("login fail:" + loginrs.getReasonId() + "--"
					+ loginrs.getMsg());
			throw new BuildException("login fail");
		}
		// 按月上传
		List<String> pathList = new ArrayList<String>();
		pathList = findScriptFile(filepath);
		// 批次新增
		if (objIsNotNull(pathList)) {
			for (String path : pathList) {
				log.info("upload begin--path:" + path);
				String uuid = UUID.randomUUID().toString().replace("-", "");// 流水号
				TxnElementModel txnElement = new TxnElementModel();
				txnElement.setAppNo(clientAppid);
				txnElement.setTxnNo(uuid);
				txnElement.setCreateDate(today);
				txnElement.setBatchModelCode("STMS_DOC");
				txnElement.setTxnType("STMS");
				List<FileElementModel> fileModelList = new ArrayList<FileElementModel>();// 文档模型
				// 电子凭证只扫描pdf
					try {
						Collection<File> listFile = FileUtils.listFiles(
								new File(path),
								FileFilterUtils.suffixFileFilter(filetype),
								DirectoryFileFilter.INSTANCE);
						if (objIsNotNull(listFile)) {
							for (File file : listFile) {
								FileElementModel fileModel = new FileElementModel();
								fileModel.setFileModelCode("STMS_PART");
								fileModel.setFileType(filetype);
								fileModel.setNodeSeq("0");
								fileModel.setImageSeq("1");
								fileModel.setFilePath(file.getAbsolutePath());
								fileModelList.add(fileModel);
							}
						} else {
							File file =new File(path);
							log.info("upload's dir:"+path+" is empty");
							if (file.exists()) {
								log.info("delete dir :"
										+ file.getAbsolutePath());
								FileUtils.deleteDirectory(file);// 删除文件
							}
							continue;
						}
						IsrpResult result = isrpClientApi.createBatch(
								tokenModel, txnElement, fileModelList);
						if (objIsNotNull(result)
								&& "0000".equals(result.getReasonId().trim())) {
							log.info("upload success");
							ResultModel rm = (ResultModel) result.getObj();
							boolean dbsucess = false;// 数据库操作是否成功
							for (File file : listFile) {
								try {
									String sql = "insert into ISRP_MOVE_MSG(txid,txno,filepath,filename,createdate) values(?,?,?,?,?)";
									int i = conn.update(
											sql,
											new String[] { rm.getBatchId(),
													uuid,
													file.getAbsolutePath(),
													file.getName(), today });
									if (i > 0) {
										log.info("db insert "
												+ file.getAbsolutePath()
												+ " success");
										dbsucess = true;
										if (file.getParentFile().exists()) {
											log.info("delete dir :"
													+ file.getParentFile()
															.getAbsolutePath());
											FileUtils.deleteDirectory(file
													.getParentFile());// 删除文件
										}
									}
								} catch (Exception e) {
									throw new BuildException("db insert fail"
											+ e.getMessage());
								}
							}
							if (dbsucess) {
								log.info("db insert success");
								getProject().setUserProperty(this.response,
										"success");
							} else {
								log.info("db insert fail");
								getProject().setUserProperty(this.response,
										"fail");
							}
						} else {
							log.info("upload's dir:"+path+" fail--" + result.getReasonId()
									+ "--reason:" + result.getMsg());
							continue;
						}
					} catch (Exception e) {
						log.error(e.getMessage());
					}

			}
		}
		// 登出
		if (inflag) {
			String result = isrpClientApi.logoutIsrp(tokenModel.getToken());
			if (objIsNotNull(result) && result.contains("处理成功")) {
				log.info("logout success");
				return;
			}
		}

	}

	public boolean objIsNotNull(Object obj) {
		boolean flag = false;
		if (obj instanceof String) {
			String string = (String) obj;
			if (string != null && !"".equals(string.trim())
					&& !"null".equalsIgnoreCase(string)) {
				flag = true;
			}
		} else if (obj instanceof List) {
			List list = (List) obj;
			if (list != null && list.size() > 0) {
				flag = true;
			}
		} else if (obj instanceof Map) {
			Map map = (Map) obj;
			if (map != null && map.size() > 0) {
				flag = true;
			}
		} else {
			if (obj != null) {
				flag = true;
			}
		}
		return flag;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public void setDataSource(String dataSource) {
		if ((dataSource == null) || (dataSource.trim().length() == 0))
			return;
		this.datasource = dataSource;
	}

	public void setClientIp(String clientIp) {
		if ((clientIp == null) || (clientIp.trim().length() == 0))
			return;
		this.clientIp = clientIp;
	}

	public void setClientPort(String clientPort) {
		if ((clientPort == null) || (clientPort.trim().length() == 0))
			return;
		this.clientPort = clientPort;
	}

	public void setClientName(String clientName) {
		if ((clientName == null) || (clientName.trim().length() == 0))
			return;
		this.clientName = clientName;
	}

	public void setClientAppid(String clientAppid) {
		if ((clientAppid == null) || (clientAppid.trim().length() == 0))
			return;
		this.clientAppid = clientAppid;
	}

	public void setClientUserid(String clientUserid) {
		if ((clientUserid == null) || (clientUserid.trim().length() == 0))
			return;
		this.clientUserid = clientUserid;
	}

	public void setClientPwd(String clientPwd) {
		if ((clientPwd == null) || (clientPwd.trim().length() == 0))
			return;
		this.clientPwd = clientPwd;
	}

	public void setClientjgh(String clientjgh) {
		if ((clientjgh == null) || (clientjgh.trim().length() == 0))
			return;
		this.clientjgh = clientjgh;
	}

	public void setClientfrh(String clientfrh) {
		if ((clientfrh == null) || (clientfrh.trim().length() == 0))
			return;
		this.clientfrh = clientfrh;
	}

	public void setClientgyh(String clientgyh) {
		if ((clientgyh == null) || (clientgyh.trim().length() == 0))
			return;
		this.clientgyh = clientgyh;
	}

	public void setFilepath(String filepath) {
		if ((filepath == null) || (filepath.trim().length() == 0))
			return;
		this.filepath = filepath;
	}

	public void setFiletype(String filetype) {
		if ((filetype == null) || (filetype.trim().length() == 0))
			return;
		this.filetype = filetype;
	}

	// 数据库连接
	private JdbcTemplate getDBConnection() throws BuildException {
		try {
			if ((objIsNotNull(this.datasource))
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

	// 批次新增
	/**
	 * 从电子凭证的根目录找出不是当月文件夹的目录
	 * 
	 * @param path
	 *            电子凭证的根目录
	 * 
	 * @return 目录列表
	 */
	public List<String> findScriptFile(String path) {
		String curMonth = new SimpleDateFormat("YYYYMM").format(new Date());
		List<String> pathlist = new ArrayList<String>();
		File baseDir = new File(path); // 根目录
		if (!baseDir.exists() || baseDir.isFile()) {
			return null;
		}
		// 查找此目录是否存在三级目录（日期目录）

		FileFilter oneDir = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory();
			}
		};
		File[] dirs = baseDir.listFiles(oneDir); // 二级目录(渠道)
		if (dirs != null) {
			FileFilter towDir = new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory();
				}
			};
			for (File tmp : dirs) {
				File[] three = tmp.listFiles(towDir); // 三级目录(渠道)
				if (three != null) {
					for (File ff : three) {
						if (objIsNotNull(ff.getName())
								&& !curMonth.equals((ff.getName().trim())
										.substring(0, 6))) {
							pathlist.add(ff.getAbsolutePath());
						}

					}
				}
			}
		}
		return pathlist;
	}
}
