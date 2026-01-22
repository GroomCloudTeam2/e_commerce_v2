package com.groom.e_commerce.global.domain.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class BaseEntityTest {

	static class TestEntity extends BaseEntity {
	}

	@Test
	void softDelete_setsDeletedAtAndDeletedBy() {
		TestEntity entity = new TestEntity();

		entity.softDelete("admin");

		assertThat(entity.getDeletedAt()).isNotNull();
		assertThat(entity.getDeletedBy()).isEqualTo("admin");
	}
}
