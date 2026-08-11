package com.example.DevConnect.enums;

/**
 * Which of a user's two addresses notifications are delivered to.
 *
 * PRIMARY is the sign-in address; SECONDARY is a delivery-only address that never
 * identifies the account. Null on existing rows is read as PRIMARY.
 */
public enum EmailPreference {
    PRIMARY,
    SECONDARY
}
