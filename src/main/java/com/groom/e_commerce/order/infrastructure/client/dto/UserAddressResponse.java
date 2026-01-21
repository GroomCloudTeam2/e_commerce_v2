package com.groom.e_commerce.order.infrastructure.client.dto;

import lombok.Getter;

@Getter
public class UserAddressResponse {

	private String recipientName;
	private String recipientPhone;
	private String zipCode;
	private String address;
	private String detailAddress;
}
