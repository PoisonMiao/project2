package com.ifchange.tob.common.view;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ifchange.tob.common.core.IRequest;
import com.ifchange.tob.common.core.RpcReply;
import com.ifchange.tob.common.helper.CollectsHelper;
import com.ifchange.tob.common.helper.DateHelper;
import com.ifchange.tob.common.helper.JsonHelper;
import com.ifchange.tob.common.helper.NetworkHelper;
import com.ifchange.tob.common.helper.SnowIdHelper;
import com.ifchange.tob.common.helper.SpringHelper;
import com.ifchange.tob.common.helper.StringHelper;
import com.ifchange.tob.common.helper.ThreadFactoryHelper;
import com.ifchange.tob.common.support.CommonCode;
import com.ifchange.tob.common.support.IConstant;
import com.ifchange.tob.common.support.OperationLog;
import com.ifchange.tob.common.view.parser.ApiOperation;
import com.ifchange.tob.common.view.parser.RequestContext;
import com.ifchange.tob.common.view.parser.RequestSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Aspect
@Order(0)
public final class ApiOperationInterceptor {
	private static final Logger LOG = LoggerFactory.getLogger(ApiOperationInterceptor.class);
	private static final Logger OUT = LoggerFactory.getLogger(OperationLog.class);
	private static final ExecutorService LOG_EXECUTOR = Executors.newSingleThreadExecutor(ThreadFactoryHelper.threadFactoryOf("operation-log"));
	private static final Map<Method, List<MethodV>> parameterMap = Maps.newHashMap();
	private static final String BODY_OBJ = "RequestBody", IGNORES = "0";
	private final ApplicationContext context;

	ApiOperationInterceptor(ApplicationContext context) {
		this.context = context;
	}

	@Around("@annotation(operation)")
	public Object around(ProceedingJoinPoint joinPoint, ApiOperation operation) throws Throwable {
		Object response;
		Object[] args = joinPoint.getArgs();
		try {
			verifyArgs(args);
			response = joinPoint.proceed(joinPoint.getArgs());
			operationLog(joinPoint, operation, args, response);
		} catch (Throwable e) {
			operationLog(joinPoint, operation, args, e);
			throw e;
		}
		return response;
	}

	/** 校验参数 **/
	private void verifyArgs(Object[] args) {
		if(!CollectsHelper.isNullOrEmpty(args)) {
			for(Object arg: args)
				if (arg instanceof IRequest) {
					((IRequest) arg).verify();
				}
		}
	}

	/** 记录操作日志 **/
	private void operationLog(final ProceedingJoinPoint joinPoint, final ApiOperation operation, final Object[] args, final Object reply) {
		if(!"true".equalsIgnoreCase(context.getEnvironment().getProperty(IConstant.KEY_OPERATIONS_LOG_ENABLE))) {
			return;
		}
		if(!operation.note()) {
			return;
		}
		final Method method = joinPointMethod(joinPoint);
		if(method.getReturnType().equals(Void.TYPE)) {
			return;
		}
		final String body = RpcReply.Helper.get().getBody();
		final RequestSession session = RequestContext.get().getSession();
		//异步处理操作日志
		LOG_EXECUTOR.submit(() -> {
			try {
				if(StringHelper.isBlank(body)) {
					List<MethodV> parameters = parameterList(method);
					Object requestBody = StringHelper.EMPTY;
					for(int i = 0; i < parameters.size(); i++){
						MethodV np = parameters.get(i);
						// Request Body Data
						if(BODY_OBJ.equals(np.name)) {
							if(null != args[i]) {
								requestBody = args[i];
							}
							break;
						}
						if(IGNORES.equals(np.name)) {
							if(LOG.isDebugEnabled()) {
								LOG.debug("operation log ignore args: {}", (i + 1));
							}
						}
					}
					OUT.info("{}", JsonHelper.toJSONString(createOperationLog(reply, session, operation, requestBody)));
				} else {
					OUT.info("{}", JsonHelper.toJSONString(createOperationLog(reply, session, operation, body)));
				}
			} catch (Exception e) {
				LOG.error("record operation log error ", e);
			}
		});
	}

	//获取拦截方法
	private Method joinPointMethod(ProceedingJoinPoint joinPoint) {
		return ((MethodSignature)joinPoint.getSignature()).getMethod();
	}

	//获取方法参数
	private List<MethodV> parameterList(Method method) {
		List<MethodV> parameters = parameterMap.get(method);
		if(null == parameters) {
			synchronized(parameterMap) {
				parameters = parameterMap.get(method);
				if(null == parameters) {
					parameters = Lists.newArrayList();
					Parameter[] ps = method.getParameters();
					if(!CollectsHelper.isNullOrEmpty(ps)) {
						for (Parameter p : ps) {
							RequestBody bf = p.getAnnotation(RequestBody.class);
							if(null != bf) {
								parameters.add(new MethodV(BODY_OBJ, BODY_OBJ));
							} else {
								//没有注解的参数全部忽略
								parameters.add(new MethodV(IGNORES, IGNORES));
							}
						}
					}
					parameterMap.put(method, parameters);
				}
			}
		}
		return parameters;
	}

	//生成操作日志
	private OperationLog createOperationLog(Object reply, RequestSession session, ApiOperation operation, Object requestBody) {
		OperationLog log = new OperationLog();
		log.logId = SnowIdHelper.nextId();
		log.rid = session.rid;
		log.apiName = operation.name();
		log.accessTime = DateHelper.doubleTime(session.accessTime);

		log.uri = session.uri;
		log.query = session.query;
		log.domain = session.domain;

		log.clientIp = session.clientIp;
		log.userAgent = session.userAgent;
		log.signature = session.signature;
		log.setRequestBody(requestBody);

		log.uid = session.uid;
		log.tid = session.tid;

		log.sn = SpringHelper.applicationName();
		log.env = SpringHelper.applicationEnv();
		log.host = NetworkHelper.localHostName();
		log.serverIp = NetworkHelper.machineIP();

		if(reply instanceof RpcReply) {
			log.setResponseBody(reply);
			log.status = ((RpcReply)reply).response.errorNo == CommonCode.SuccessOk.code();
		} else if(reply instanceof Throwable) {
			log.status = false;
			log.setResponseBody(((Throwable)reply).getMessage());
		} else {
			log.status = true;
			log.setResponseBody(reply);
		}
		log.cost = DateHelper.time() - session.accessTime;
		return log;
	}

	private final class MethodV {
		String id;
		String name;

		MethodV(String id, String name){
			this.id = id;
			this.name = name;
		}
	}
}
