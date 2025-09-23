package fr.onitsag.faritech.init;

import fr.onitsag.faritech.recipe.RecipeCutPaper;
import fr.onitsag.faritech.recipe.RecipeLaptop;
import fr.onitsag.faritech.recipe.RecipeMotherboard;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class DeviceCrafting
{
	public static void register()
	{
		RegistrationHandler.Recipes.add(new RecipeCutPaper());
		RegistrationHandler.Recipes.add(new RecipeMotherboard());
		RegistrationHandler.Recipes.add(new RecipeLaptop());
		GameRegistry.addSmelting(DeviceItems.PLASTIC_UNREFINED, new ItemStack(DeviceItems.PLASTIC, 4), 0.2F);
	}
}
