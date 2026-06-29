-- pg_trgm % 연산자 임계값을 0.2로 설정
-- 기본값(0.3)은 짧은 한글 키워드(예: '이디야') 검색 시 유일한 정답을 누락시킴
ALTER DATABASE cafun SET pg_trgm.similarity_threshold = 0.2;
