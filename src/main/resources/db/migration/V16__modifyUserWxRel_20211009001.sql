DROP index if exists userWxRelationUserId;
DROP index if exists userWxRelationAppId;

create unique index if not exists userWxRelationUserIdAppId on user_wx_relation (user_id, app_id);