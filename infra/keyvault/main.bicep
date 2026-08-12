// Azure Key Vault for the PDP baseline, one instance per environment.
//
// This template creates the vault and grants access to it. It deliberately does NOT create secret
// values: a secret written in a template is a secret committed to Git, which is exactly the
// problem the vault exists to solve. Values are loaded once, out of band, by whoever owns them.

targetScope = 'resourceGroup'

@description('Environment discriminator used in resource names: dev, qa or prod.')
@allowed(['dev', 'qa', 'prod'])
param environment string

@description('Azure region. Defaults to the resource group location.')
param location string = resourceGroup().location

@description('Object id of the managed identity the application runs as. It receives read-only access to the secrets.')
param applicationPrincipalId string

@description('Object id of the service principal behind the Azure DevOps service connection, used by the pipeline to read secrets at deploy time.')
param pipelinePrincipalId string

@description('Log Analytics workspace for audit logs. Leave empty to skip diagnostics.')
param logAnalyticsWorkspaceId string = ''

@description('Appended to the default vault name (kv-pdp-<environment>). Needed when the default name is held by a soft-deleted, purge-protected vault from a prior region migration.')
param nameSuffix string = ''

var keyVaultName = 'kv-pdp-${environment}${nameSuffix}'

// Built-in role. "Secrets User" grants read of secret values and nothing else — no write, no
// management, no access to keys or certificates.
var keyVaultSecretsUserRoleId = '4633458b-17de-408a-b874-0445c86b69e6'

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    tenantId: subscription().tenantId

    // RBAC instead of access policies: permissions become auditable role assignments that live
    // with the rest of the subscription's access model.
    enableRbacAuthorization: true

    // Recovery guarantees. Purge protection is irreversible once enabled, which is the point:
    // it prevents an attacker (or a mistake) from destroying secrets permanently.
    enableSoftDelete: true
    softDeleteRetentionInDays: environment == 'prod' ? 90 : 7
    enablePurgeProtection: true

    enabledForDeployment: false
    enabledForTemplateDeployment: false
    enabledForDiskEncryption: false

    publicNetworkAccess: 'Enabled'
    networkAcls: {
      bypass: 'AzureServices'
      defaultAction: 'Allow'
    }
  }
  tags: {
    environment: environment
    system: 'pdp'
    managedBy: 'bicep'
  }
}

resource applicationSecretsRead 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  scope: keyVault
  name: guid(keyVault.id, applicationPrincipalId, keyVaultSecretsUserRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', keyVaultSecretsUserRoleId)
    principalId: applicationPrincipalId
    principalType: 'ServicePrincipal'
  }
}

// Only create pipeline access if it's a different principal than the application
resource pipelineSecretsRead 'Microsoft.Authorization/roleAssignments@2022-04-01' = if (pipelinePrincipalId != applicationPrincipalId) {
  scope: keyVault
  name: guid(keyVault.id, pipelinePrincipalId, keyVaultSecretsUserRoleId)
  properties: {
    roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', keyVaultSecretsUserRoleId)
    principalId: pipelinePrincipalId
    principalType: 'ServicePrincipal'
  }
}

// Every read and every failed attempt is recorded. Without this, a leaked credential is
// undetectable.
resource diagnostics 'Microsoft.Insights/diagnosticSettings@2021-05-01-preview' = if (!empty(logAnalyticsWorkspaceId)) {
  scope: keyVault
  name: 'kv-audit'
  properties: {
    workspaceId: logAnalyticsWorkspaceId
    logs: [
      {
        category: 'AuditEvent'
        enabled: true
      }
    ]
    metrics: [
      {
        category: 'AllMetrics'
        enabled: true
      }
    ]
  }
}

output keyVaultName string = keyVault.name
output keyVaultUri string = keyVault.properties.vaultUri
