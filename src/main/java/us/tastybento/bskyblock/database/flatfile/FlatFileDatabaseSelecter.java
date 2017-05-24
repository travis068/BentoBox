package us.tastybento.bskyblock.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.DatabaseConnecter;


/**
 * 
 * Class that creates a list of <T>s filled with values from the corresponding
 * database-table.
 * 
 * @author tastybento
 * 
 * @param <T>
 */
public class FlatFileDatabaseSelecter<T> extends AbstractDatabaseHandler<T> {

    public FlatFileDatabaseSelecter(BSkyBlock plugin, Class<T> type,
            DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
    }

    @Override
    protected String createQuery() {
        return "";
    }

    /**
     * Creates a <T> filled with values from the corresponding
     * database file
     * 
     * @return <T> filled with values from the corresponding database file
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public T selectObject(String key) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException  {
        YamlConfiguration config = databaseConnecter.loadYamlFile(key);
        return createObject(config);
    }

    /**
     * 
     * Creates a list of <T>s filled with values from the provided ResultSet
     * 
     * @param config - YAML config file
     * 
     * @return <T> filled with values
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private T createObject(YamlConfiguration config) throws InstantiationException, IllegalAccessException,
    IntrospectionException, IllegalArgumentException, InvocationTargetException
    {

        T instance = type.newInstance();

        for (Field field : type.getDeclaredFields()) {

            /* We assume the table-column-names exactly match the variable-names of T */
            // TODO: depending on the data type, it'll need deserializing
            try {

                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                        field.getName(), type);

                Method method = propertyDescriptor.getWriteMethod();
                plugin.getLogger().info("DEBUG: " + field.getName() + ": " + propertyDescriptor.getPropertyType().getTypeName());
                if (propertyDescriptor.getPropertyType().equals(HashMap.class)) {
                    plugin.getLogger().info("DEBUG: is HashMap");
                    // TODO: this may not work with all keys. Further serialization may be required.
                    HashMap<Object,Object> value = new HashMap<Object, Object>();
                    for (String key : config.getConfigurationSection(field.getName()).getKeys(false)) {
                        value.put(key, config.get(field.getName() + "." + key));
                    }
                    method.invoke(instance, value);
                } else if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                    plugin.getLogger().info("DEBUG: is Set " + propertyDescriptor.getReadMethod().getGenericReturnType().getTypeName());

                    // TODO: this may not work with all keys. Further serialization may be required.
                    Set<Object> value = new HashSet((List<Object>) config.getList(field.getName()));
                    
                    method.invoke(instance, value);
                } else if (propertyDescriptor.getPropertyType().equals(UUID.class)) {
                    plugin.getLogger().info("DEBUG: is UUID");
                    String uuid = (String)config.get(field.getName());
                    if (uuid == null || uuid.equals("null")) {
                        method.invoke(instance, (Object)null); 
                    } else {
                        Object value = UUID.fromString(uuid);
                        method.invoke(instance, value);
                    }
                } else {
                    Object value = config.get(field.getName());
                    method.invoke(instance, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return instance;
    }
}
