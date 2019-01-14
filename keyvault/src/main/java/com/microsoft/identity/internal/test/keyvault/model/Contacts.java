/*
 * KeyVaultClient
 * The key vault client performs cryptographic key operations and vault operations against the Key Vault service.
 *
 * OpenAPI spec version: 2016-10-01
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.microsoft.identity.internal.test.keyvault.model;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * The contacts for the vault certificates.
 */
@ApiModel(description = "The contacts for the vault certificates.")

public class Contacts {
  @SerializedName("id")
  private String id = null;

  @SerializedName("contacts")
  private List<Contact> contacts = null;

   /**
   * Identifier for the contacts collection.
   * @return id
  **/
  @ApiModelProperty(value = "Identifier for the contacts collection.")
  public String getId() {
    return id;
  }

  public Contacts contacts(List<Contact> contacts) {
    this.contacts = contacts;
    return this;
  }

  public Contacts addContactsItem(Contact contactsItem) {
    if (this.contacts == null) {
      this.contacts = new ArrayList<Contact>();
    }
    this.contacts.add(contactsItem);
    return this;
  }

   /**
   * The contact list for the vault certificates.
   * @return contacts
  **/
  @ApiModelProperty(value = "The contact list for the vault certificates.")
  public List<Contact> getContacts() {
    return contacts;
  }

  public void setContacts(List<Contact> contacts) {
    this.contacts = contacts;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Contacts contacts = (Contacts) o;
    return Objects.equals(this.id, contacts.id) &&
        Objects.equals(this.contacts, contacts.contacts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, contacts);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Contacts {\n");
    
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    contacts: ").append(toIndentedString(contacts)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

