package com.example.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@Entity
@Table(name = "students")
public class Aluno extends Usuario {

    @Temporal(TemporalType.DATE)
    private Date birthDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date monthlyPaymentDueDate;
}
