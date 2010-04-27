cacheProps = [
    'hibernate.cache.provider_class': 'net.sf.ehcache.hibernate.SingletonEhCacheProvider',
    'hibernate.cache.region_prefix': "eucalyptus_${context_name}_cache",
    'hibernate.cache.use_second_level_cache': 'true',
    'hibernate.cache.use_query_cache': 'true',
    'hibernate.cache.use_structured_entries': 'true',
]