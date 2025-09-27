package com.dusk.module.ddm.module.auth.dto.weixin;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author jianjianhong
 * @date 2023/11/12
 */
@NoArgsConstructor
@Data
public class WxCpAppAccessTokenInfo {

    private Integer errcode;
    private String errmsg;
    private String permanent_code;
    private DealerCorpInfoDTO dealer_corp_info;
    private AuthCorpInfoDTO auth_corp_info;
    private AuthInfoDTO auth_info;
    private AuthUserInfoDTO auth_user_info;
    private String state;

    @NoArgsConstructor
    @Data
    public static class DealerCorpInfoDTO {
        private String corpid;
        private String corp_name;
    }

    @NoArgsConstructor
    @Data
    public static class AuthCorpInfoDTO {
        private String corpid;
        private String corp_name;
        private String corp_type;
        private String corp_square_logo_url;
        private Integer corp_user_max;
        private String corp_full_name;
        private Integer verified_end_time;
        private Integer subject_type;
        private String corp_wxqrcode;
        private String corp_scale;
        private String corp_industry;
        private String corp_sub_industry;
    }

    @NoArgsConstructor
    @Data
    public static class AuthInfoDTO {
        private List<AgentDTO> agent;

        @NoArgsConstructor
        @Data
        public static class AgentDTO {
            private Integer agentid;
            private String name;
            private String round_logo_url;
            private String square_logo_url;
            private Integer auth_mode;
            private Boolean is_customized_app;
            private Boolean auth_from_thirdapp;
            private PrivilegeDTO privilege;
            private SharedFromDTO shared_from;

            @NoArgsConstructor
            @Data
            public static class PrivilegeDTO {
                private Integer level;
                private List<Integer> allow_party;
                private List<String> allow_user;
                private List<Integer> allow_tag;
            }

            @NoArgsConstructor
            @Data
            public static class SharedFromDTO {
                private String corpid;
                private Integer share_type;
            }
        }
    }

    @NoArgsConstructor
    @Data
    public static class AuthUserInfoDTO {
        private String userid;
        private String open_userid;
        private String name;
        private String avatar;
    }
}
