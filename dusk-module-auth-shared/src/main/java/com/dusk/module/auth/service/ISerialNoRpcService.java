package com.dusk.module.auth.service;


import com.dusk.module.auth.dto.serialno.SerialNoEditRpcInput;
import com.dusk.module.auth.dto.serialno.SerialNoRpcDto;
import com.dusk.module.auth.enums.EnumResetType;

import java.util.List;

/**
 * 序列号生成rpc接口类，提供各种各样的序列号生成方法
 *
 * @author kefuming
 * @date 2020-09-22 15:43
 */
public interface ISerialNoRpcService {
    /**
     * 批量生成序列号  永远数据库优先
     *
     * @param billType     单据类型 （ 字符串 可以自己定义 动态拼接都可以）
     * @param resetType    流水号重置类型  天/月/年/从不
     * @param dateFormat   日期格式化，不需要格式化的参数用单引号括起来 例如 yy-MM-dd'HH' 如果为空 则不拼接 ，自定义模式
     * @param serialLength 序列号长度不够的用0左拼接
     * @param count        一次生成的序列号数量
     * @return
     */
    String[] getSerialNos(String billType, EnumResetType resetType, String dateFormat, int serialLength, int count);


    /**
     * 批量生成序列号  永远数据库优先
     *
     * @param billType     单据类型 （ 字符串 可以自己定义 动态拼接都可以）
     * @param resetType    流水号重置类型  天/月/年/从不
     * @param dateFormat   日期格式化，不需要格式化的参数用单引号括起来 例如 yy-MM-dd'HH' 如果为空 则不拼接 ，自定义模式
     * @param serialLength 序列号长度不够的用0左拼接
     * @param count        一次生成的序列号数量
     * @param codeFirst    是否代码配置优先
     * @return
     */
    String[] getSerialNos(String billType, EnumResetType resetType, String dateFormat, int serialLength, int count, boolean codeFirst);

    /**
     * 获取一个序列号  数据库配置优先
     *
     * @param billType     单据类型 （ 字符串 可以自己定义 动态拼接都可以）
     * @param resetType    流水号重置类型  天/月/年/从不
     * @param dateFormat   日期格式化，不需要格式化的参数用单引号括起来 例如 yy-MM-dd'HH' 如果为空 则不拼接 ，自定义模式
     * @param serialLength 序列号长度不够的用0左拼接
     * @return
     */
    String getSerialNo(String billType, EnumResetType resetType, String dateFormat, int serialLength);


    /**
     * 获取一个序列号  代码配置优先
     *
     * @param billType     单据类型 （ 字符串 可以自己定义 动态拼接都可以）
     * @param resetType    流水号重置类型  天/月/年/从不
     * @param dateFormat   日期格式化，不需要格式化的参数用单引号括起来 例如 yy-MM-dd'HH' 如果为空 则不拼接 ，自定义模式
     * @param serialLength 序列号长度不够的用0左拼接
     * @param codeFirst    是否代码配置优先
     * @return
     */
    String getSerialNo(String billType, EnumResetType resetType, String dateFormat, int serialLength, boolean codeFirst);

    /**
     * 获取当前数列号数据
     *
     * @param billType 单据类型 （ 字符串 可以自己定义 动态拼接都可以）
     * @return
     */
    default List<SerialNoRpcDto> getSerialNos(String billType) {
        throw new UnsupportedOperationException();
    }

    /**
     * 更新
     *
     * @param input
     */
    default void updateSerialNo(SerialNoEditRpcInput input) {
        throw new UnsupportedOperationException();
    }

}
