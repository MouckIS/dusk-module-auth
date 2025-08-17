package com.dusk.module.auth.common.skiprequest;

import com.dusk.module.auth.common.permission.IPermissionCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * @author kefuming
 * @date 2020-05-25 16:40
 */
@Component
public class SkipPathRequestMatcher {

    @Autowired
    IPermissionCache permissionCache;


    public boolean matches(String applicationName, String url){
        List<String> anonymousPath = permissionCache.getAllowAnonymousPath().get(applicationName);
        AntPathMatcher pathMatcher=new AntPathMatcher();
        if(anonymousPath != null && anonymousPath.size()>0){
            for(String path:anonymousPath){
                if(pathMatcher.match(path,url)){
                    return true;
                }
            }
        }
        return false;
    }
}
