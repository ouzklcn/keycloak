package org.keycloak.services.models.nosql.impl.types;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import org.keycloak.services.models.nosql.api.AttributedNoSQLObject;
import org.keycloak.services.models.nosql.api.NoSQLObject;
import org.keycloak.services.models.nosql.api.types.Converter;
import org.keycloak.services.models.nosql.api.types.TypeConverter;
import org.keycloak.services.models.nosql.impl.MongoDBImpl;
import org.keycloak.services.models.nosql.impl.ObjectInfo;
import org.picketlink.common.properties.Property;
import org.picketlink.common.reflection.Types;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class NoSQLObjectConverter<T extends NoSQLObject> implements Converter<T, BasicDBObject> {

    private final MongoDBImpl mongoDBImpl;
    private final TypeConverter typeConverter;
    private final Class<T> expectedNoSQLObjectType;

    public NoSQLObjectConverter(MongoDBImpl mongoDBImpl, TypeConverter typeConverter, Class<T> expectedNoSQLObjectType) {
        this.mongoDBImpl = mongoDBImpl;
        this.typeConverter = typeConverter;
        this.expectedNoSQLObjectType = expectedNoSQLObjectType;
    }

    @Override
    public BasicDBObject convertObject(T applicationObject) {
        ObjectInfo objectInfo = mongoDBImpl.getObjectInfo(applicationObject.getClass());

        // Create instance of BasicDBObject and add all declared properties to it (properties with null value probably should be skipped)
        BasicDBObject dbObject = new BasicDBObject();
        Collection<Property<Object>> props = objectInfo.getProperties();
        for (Property<Object> property : props) {
            String propName = property.getName();
            Object propValue = property.getValue(applicationObject);

            Object dbValue = propValue == null ? null : typeConverter.convertApplicationObjectToDBObject(propValue, Types.boxedClass(property.getJavaClass()));
            dbObject.put(propName, dbValue);
        }

        // Adding attributes
        if (applicationObject instanceof AttributedNoSQLObject) {
            AttributedNoSQLObject attributedObject = (AttributedNoSQLObject)applicationObject;
            Map<String, String> attributes = attributedObject.getAttributes();
            for (Map.Entry<String, String> attribute : attributes.entrySet()) {
                dbObject.append(attribute.getKey(), attribute.getValue());
            }
        }

        return dbObject;
    }

    @Override
    public Class<? extends T> getConverterObjectType() {
        return expectedNoSQLObjectType;
    }

    @Override
    public Class<BasicDBObject> getExpectedReturnType() {
        return BasicDBObject.class;
    }
}
