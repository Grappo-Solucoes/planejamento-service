/**
 * Módulo Shared Kernel (Núcleo Compartilhado).
 *
 * Contém elementos genéricos e reutilizáveis por todos os demais módulos:
 * - Value Objects compartilhados (Endereco, Email, Telefone, CpfCnpj)
 * - Interfaces base (AggregateRoot, DomainEvent)
 * - Utilidades e abstrações comuns
 * - Configurações de infraestrutura compartilhada
 *
 * Este módulo é do tipo OPEN, o que significa que:
 * - Todos os seus componentes são acessíveis por qualquer outro módulo
 * - Não há restrições de encapsulamento (é propositalmente aberto)
 * - Serve como biblioteca de base para o domínio
 *
 * @since 1.0
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Kernel",
        type = ApplicationModule.Type.OPEN  // Permite acesso irrestrito a todos os outros módulos
)
package br.com.busco.planejamento.sk;

import org.springframework.modulith.ApplicationModule;
