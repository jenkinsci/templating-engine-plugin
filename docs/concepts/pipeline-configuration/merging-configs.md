# Merging Configuration Files

During [Pipeline Initialization](../advanced/pipeline-initialization.md), JTE collects every Pipeline Configuration in the [Configuration Hierarchy](../pipeline-governance/configuration-hierarchy.md) for the Pipeline Run.

This Pipeline Configuration chain is then sequentially merged, starting with the top-most configuration and ending with the most granular.

The following guidelines explain how two Pipeline Configurations are merged together.

## The First Pipeline Configuration

The first Pipeline Configuration in the configuration chain can define any blocks and properties.

## Merging When A Parent Pipeline Configuration Is Present

After the first Pipeline Configuration has been set, each subsequent Pipeline Configuration can define root-level blocks and properties but **can't** modify properties or blocks that were previously set unless **explicitly** permitted by the previous configuration.

### Permitting Modifications

Pipeline Configurations must explicitly define which blocks and properties can be modified by the next configuration in the configuration chain.

This is done through the `@override` and `@merge` annotations.

### `@override`

The `@override` annotation is used to permit block-level changes or to permit specific properties to be changed.

Setting `@override` on a block will allow the next configuration to change any property in the block.

Setting `@override` on a property will allow the next configuration to change that property.

### `@merge`

The `@merge` annotation is used at the block-level to allow the next configuration in the configuration chain to append properties to the block but not change inherited properties.
