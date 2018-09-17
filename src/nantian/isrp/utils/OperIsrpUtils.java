package nantian.isrp.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import mp.isrpapi.IsrpClientApi;
import mp.isrpapi.isrpApiImpl.IsrpClientApiHttpImpl;
import mp.model.FileElementModel;
import mp.model.InitModel;
import mp.model.IsrpResult;
import mp.model.ResultModel;
import mp.model.TxnElementModel;

/**
 **************************************************************** 
 * 公司名称 :广州南天电脑系统有限公司 系统名称 :stmf 功能描述 :操作内容平台api的工具类 业务描述 : 作 者 名 :@sky_zhu 开发日期
 * :2018年4月8日 下午4:21:27
 ***************************************************************
 * 修改日期 : 修 改 者 : 修改内容 :
 ***************************************************************
 */
public class OperIsrpUtils {
	 private Logger logger = Logger.getLogger("Scheduler");
	private IsrpClientApi isrpClientApi;
	private String clientIp = null;// 连接内容平台的ip
	private String clientPort = null;// 端口号
	private String clientName = null;// 服务名
	private String clientAppid = null;// 服务id
	private String clientUserid = null;// 用户id
	private String clientPwd = null;// 用户密码
	private String clientjgh = null;// 机构号
	private String clientfrh = null;// 法人代码
	private String clientgyh = null;// 法人代码
	private InitModel tokenModel;// 共用的token对象

	/**
	 * @param clientIp
	 * @param clientPort
	 * @param clientName
	 * @param clientAppid
	 * @param clientUserid
	 * @param clientPwd
	 * @param clientjgh
	 * @param clientfrh
	 * @param clientgyh
	 */
	public OperIsrpUtils(String clientIp, String clientPort, String clientName,
			String clientAppid, String clientUserid, String clientPwd,
			String clientjgh, String clientfrh, String clientgyh) {
		this.clientIp = clientIp;
		this.clientPort = clientPort;
		this.clientName = clientName;
		this.clientAppid = clientAppid;
		this.clientUserid = clientUserid;
		this.clientPwd = clientPwd;
		this.clientjgh = clientjgh;
		this.clientfrh = clientfrh;
		this.clientgyh = clientgyh;
		// 初始化api
		isrpClientApi = new IsrpClientApiHttpImpl(clientIp, clientPort,
				clientName, "");
	}

	// 登录
	protected boolean login() {
		boolean flag = false;
		IsrpResult rs = isrpClientApi.loginIsrp(clientUserid, clientPwd,
				clientjgh, clientfrh, clientAppid, clientgyh);
		if ("0000".equals(rs.getReasonId().trim())) {
			flag = true;
			tokenModel = (InitModel) rs.getObj();
		}
		return flag;
	}

	// 登出
	protected boolean logout() {
		boolean out = false;
		if (objIsNotNull(isrpClientApi) && objIsNotNull(tokenModel)) {
			String result = isrpClientApi.logoutIsrp(tokenModel.getToken());
			if (objIsNotNull(result) && result.contains("处理成功")) {
				out = true;
			}
		}
		return out;
	}

	// 批次新增
	protected void newInsert(String filepath, String filetype) {
		String uuid = UUID.randomUUID().toString().replace("-", "");// 流水号
		String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
		TxnElementModel txnElement = new TxnElementModel();
		txnElement.setAppNo(clientAppid);
		txnElement.setTxnNo(uuid);
		txnElement.setCreateDate(today);
		txnElement.setBatchModelCode("STMS_DOC");
		txnElement.setTxnType("STMS");
		System.out.println();
		List<FileElementModel> fileModelList = new ArrayList<FileElementModel>();// 文档模型
		// 电子凭证只扫描pdf
		if (objIsNotNull(filepath)) {
			try {
				Collection<File> listFile = FileUtils.listFiles(new File(
						filepath), FileFilterUtils.suffixFileFilter(filetype),
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
					return;
				}
				IsrpResult result = isrpClientApi.createBatch(tokenModel,
						txnElement, fileModelList);
				if (objIsNotNull(result)
						&& "0000".equals(result.getReasonId().trim())) {
					ResultModel rm = (ResultModel) result.getObj();
					System.out.println("流水号：" + uuid + result.getReasonId()
							+ ":批次号：" + rm.getBatchId());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 批次查询
	protected File getFile(String txnNo, String fileName, String createDate) {
		File file = null;
		TxnElementModel txnElement = new TxnElementModel();
		txnElement.setAppNo(clientAppid);
		txnElement.setTxnNo(txnNo);
		txnElement.setCreateDate(createDate);
		txnElement.setBatchModelCode("STMS_DOC");
		txnElement.setTxnType("STMS");
		IsrpResult rs = isrpClientApi.queryBatch(tokenModel, txnElement);
		if (objIsNotNull(rs)) {
			if ("0000".equals(rs.getReasonId())) {
				Map<String, Object> filemsg = (Map<String, Object>) rs.getObj();
				List<FileElementModel> fe = (List<FileElementModel>) filemsg
						.get("fileElementModel");
				if (fe.size() != 0) {
					for (FileElementModel fm : fe) {
						if (objIsNotNull(fileName)
								&& fileName.equals(fm.getFileSaveName())) {
							file = downLoad(fm.getUrlPath(),
									fm.getFileSaveName());
						} else {
							logger.warn("the file is not found");
						}
					}
				}
			} else {
				logger.warn("the file query fail" + rs.getMsg());
			}

		}
		return file;
	}

	// 文件下载
	protected File downLoad(String fileURL, String fileName) {
		if (objIsNotNull(fileName) && objIsNotNull(fileURL)) {
			IsrpResult rf = isrpClientApi.downLoadFile(fileURL, fileName);
			if (objIsNotNull(rf)) {
				if ("0000".equals(rf.getReasonId())) {
					File file = (File) rf.getObj();
					return file;
				} else {
					logger.warn("file download fail" + rf.getMsg());
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @author zhuchaopei
	 * @DateTime 2018年4月9日 上午9:40:37
	 * @serverCode 服务代码
	 * @serverComment 判断对象是否为空
	 *
	 * @param obj
	 * @return
	 */
	protected boolean objIsNotNull(Object obj) {
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
}
