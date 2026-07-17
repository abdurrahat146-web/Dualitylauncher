#pragma once

#include <glad/gl.h>
#include <string>
#include <fstream>
#include <sstream>
#include <iostream>

class Shader
{
public:
    unsigned int ID = 0;

    Shader() = default;

    bool loadFromFiles(const std::string& vertexPath, const std::string& fragmentPath)
    {
        std::string vertexCode;
        std::string fragmentCode;

        if (!readFile(vertexPath, vertexCode))
        {
            std::cerr << "[Shader] Failed to read vertex shader: " << vertexPath << std::endl;
            return false;
        }

        if (!readFile(fragmentPath, fragmentCode))
        {
            std::cerr << "[Shader] Failed to read fragment shader: " << fragmentPath << std::endl;
            return false;
        }

        const char* vShaderCode = vertexCode.c_str();
        const char* fShaderCode = fragmentCode.c_str();

        unsigned int vertex = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex, 1, &vShaderCode, nullptr);
        glCompileShader(vertex);
        if (!checkCompileErrors(vertex, "VERTEX"))
        {
            glDeleteShader(vertex);
            return false;
        }

        unsigned int fragment = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment, 1, &fShaderCode, nullptr);
        glCompileShader(fragment);
        if (!checkCompileErrors(fragment, "FRAGMENT"))
        {
            glDeleteShader(vertex);
            glDeleteShader(fragment);
            return false;
        }

        ID = glCreateProgram();
        glAttachShader(ID, vertex);
        glAttachShader(ID, fragment);
        glLinkProgram(ID);
        bool linkOk = checkCompileErrors(ID, "PROGRAM");

        glDeleteShader(vertex);
        glDeleteShader(fragment);

        return linkOk;
    }

    void use() const
    {
        glUseProgram(ID);
    }

    void setBool(const std::string& name, bool value) const
    {
        glUniform1i(glGetUniformLocation(ID, name.c_str()), (int)value);
    }

    void setInt(const std::string& name, int value) const
    {
        glUniform1i(glGetUniformLocation(ID, name.c_str()), value);
    }

    void setFloat(const std::string& name, float value) const
    {
        glUniform1f(glGetUniformLocation(ID, name.c_str()), value);
    }

    void setVec3(const std::string& name, float x, float y, float z) const
    {
        glUniform3f(glGetUniformLocation(ID, name.c_str()), x, y, z);
    }

    void setMat4(const std::string& name, const float* matrix) const
    {
        glUniformMatrix4fv(glGetUniformLocation(ID, name.c_str()), 1, GL_FALSE, matrix);
    }

    void setMat3(const std::string& name, const float* matrix) const
    {
        glUniformMatrix3fv(glGetUniformLocation(ID, name.c_str()), 1, GL_FALSE, matrix);
    }

private:
    static bool readFile(const std::string& path, std::string& outContent)
    {
        std::ifstream file;
        file.exceptions(std::ifstream::failbit | std::ifstream::badbit);
        try
        {
            file.open(path);
            std::stringstream stream;
            stream << file.rdbuf();
            file.close();
            outContent = stream.str();
            return true;
        }
        catch (std::ifstream::failure&)
        {
            return false;
        }
    }

    static bool checkCompileErrors(unsigned int shader, const std::string& type)
    {
        int success;
        char infoLog[1024];

        if (type != "PROGRAM")
        {
            glGetShaderiv(shader, GL_COMPILE_STATUS, &success);
            if (!success)
            {
                glGetShaderInfoLog(shader, 1024, nullptr, infoLog);
                std::cerr << "[Shader] Compile error (" << type << "):\n" << infoLog << std::endl;
                return false;
            }
        }
        else
        {
            glGetProgramiv(shader, GL_LINK_STATUS, &success);
            if (!success)
            {
                glGetProgramInfoLog(shader, 1024, nullptr, infoLog);
                std::cerr << "[Shader] Link error:\n" << infoLog << std::endl;
                return false;
            }
        }
        return true;
    }
};
