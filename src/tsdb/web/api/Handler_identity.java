package tsdb.web.api;

import java.io.IOException;
import java.lang.reflect.Field;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.DefaultUserIdentity;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.json.JSONWriter;

import tsdb.remote.RemoteTsDB;

/**
 * get meta data of region 
 * @author woellauer
 *
 */
public class Handler_identity extends MethodHandler {	
	private static final Logger log = LogManager.getLogger();
	
	public static final Field FIELD_ROLES;

	static {
		Field fieldRoles = null;
		try {
			fieldRoles = DefaultUserIdentity.class.getDeclaredField("_roles");
			fieldRoles.setAccessible(true);
		} catch(Exception e) {}
		FIELD_ROLES = fieldRoles;		
	}
	
	public Handler_identity(RemoteTsDB tsdb) {
		super(tsdb, "identity");
	}

	@Override
	public void handle(String target, Request request, HttpServletRequest req, HttpServletResponse response) throws IOException, ServletException {		
		request.setHandled(true);
		response.setContentType("application/json;charset=utf-8");
		JSONWriter json = new JSONWriter(response.getWriter());

		String user = "anonymous";
		String authMethod = "";
		String[] roles = new String[]{};

		Authentication authentication = request.getAuthentication();
		if(authentication != null && (authentication instanceof User)) {
			User authUser = (User) authentication;
			UserIdentity userIdentity = authUser.getUserIdentity();
			user = userIdentity.getUserPrincipal().getName();
			authMethod = authUser.getAuthMethod();

			if(FIELD_ROLES != null && (userIdentity instanceof DefaultUserIdentity)) {
				try {
					roles = (String[]) FIELD_ROLES.get(userIdentity);
				} catch(Exception e) {
					log.warn(e);
				}
			}
		} else {
			roles = new String[]{"admin"};
		}

		json.object();
		json.key("ip");
		json.value(request.getRemoteAddr());
		json.key("user");
		json.value(user);
		json.key("auth_method");
		json.value(authMethod);
		json.key("roles");
		json.value(roles);
		json.endObject();		
	}
}