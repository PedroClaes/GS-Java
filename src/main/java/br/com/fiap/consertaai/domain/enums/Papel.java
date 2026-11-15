package br.com.fiap.consertaai.domain.enums;

/**
 * Papeis de acesso. Viajam no claim "roles" do JWT e viram ROLE_CIDADAO / ROLE_GESTOR
 * no Spring Security. A API .NET le o mesmo claim.
 */
public enum Papel {
    CIDADAO,
    GESTOR
}
