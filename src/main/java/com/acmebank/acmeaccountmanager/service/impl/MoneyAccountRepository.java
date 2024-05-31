package com.acmebank.acmeaccountmanager.service.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface MoneyAccountRepository extends JpaRepository<MoneyAccountEntity, String> {

}
