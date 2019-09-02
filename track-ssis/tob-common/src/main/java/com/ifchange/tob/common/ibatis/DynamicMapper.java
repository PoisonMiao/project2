package com.ifchange.tob.common.ibatis;

import com.ifchange.tob.common.cache.CachedMapper;

/** 继承 CachedMapper 允许在 Mapper 加缓存 **/
public interface DynamicMapper<T extends DynamicMapper> extends CachedMapper {
	default T withSTG(final DynamicStrategy stg) {
		if(null != stg) {
			DataSourceManager.get().setSTG(stg);
		}
		return (T) this;
	}
	default DynamicStrategy defaultSTG() {
		return DefaultStrategy.stg();
	}
}
