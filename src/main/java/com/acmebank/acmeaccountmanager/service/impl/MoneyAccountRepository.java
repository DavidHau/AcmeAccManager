package com.acmebank.acmeaccountmanager.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface MoneyAccountRepository extends JpaRepository<MoneyAccountEntity, String> {

    List<MoneyAccountEntity> findAllByPrimaryOwnerIdOrderById(UUID primaryOwnerId);
}
